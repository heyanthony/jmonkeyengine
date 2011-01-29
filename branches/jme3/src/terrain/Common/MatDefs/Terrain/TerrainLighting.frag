
uniform float m_Shininess;

varying vec4 AmbientSum;
varying vec4 DiffuseSum;
varying vec4 SpecularSum;

varying vec3 vNormal;
varying vec2 texCoord;
varying vec3 vPosition;
varying vec3 vnPosition;
varying vec3 vViewDir;
varying vec4 vLightDir;
varying vec4 vnLightDir;


#ifdef DIFFUSEMAP
  uniform sampler2D m_DiffuseMap;
#endif
#ifdef DIFFUSEMAP_1
  uniform sampler2D m_DiffuseMap_1;
#endif
#ifdef DIFFUSEMAP_2
  uniform sampler2D m_DiffuseMap_2;
#endif
#ifdef DIFFUSEMAP_3
  uniform sampler2D m_DiffuseMap_3;
#endif


#ifdef DIFFUSEMAP_0_SCALE
  uniform float m_DiffuseMap_0_scale;
#endif
#ifdef DIFFUSEMAP_1_SCALE
  uniform float m_DiffuseMap_1_scale;
#endif
#ifdef DIFFUSEMAP_2_SCALE
  uniform float m_DiffuseMap_2_scale;
#endif
#ifdef DIFFUSEMAP_3_SCALE
  uniform float m_DiffuseMap_3_scale;
#endif


#ifdef ALPHAMAP
  uniform sampler2D m_AlphaMap;
#endif
#ifdef ALPHAMAP_1
  uniform sampler2D m_AlphaMap_1;
#endif
#ifdef ALPHAMAP_2
  uniform sampler2D m_AlphaMap_2;
#endif

#ifdef NORMALMAP
  uniform sampler2D m_NormalMap;
#endif
#ifdef NORMALMAP_1
  uniform sampler2D m_NormalMap_1;
#endif
#ifdef NORMALMAP_2
  uniform sampler2D m_NormalMap_2;
#endif
#ifdef NORMALMAP_3
  uniform sampler2D m_NormalMap_3;
#endif

#ifdef TRI_PLANAR_MAPPING
  varying vec4 wVertex;
  varying vec3 wNormal;
#endif



float tangDot(in vec3 v1, in vec3 v2){
    float d = dot(v1,v2);
    #ifdef V_TANGENT
        d = 1.0 - d*d;
        return step(0.0, d) * sqrt(d);
    #else
        return d;
    #endif
}


float lightComputeDiffuse(in vec3 norm, in vec3 lightdir, in vec3 viewdir){
    return max(0.0, dot(norm, lightdir));
}

float lightComputeSpecular(in vec3 norm, in vec3 viewdir, in vec3 lightdir, in float shiny){
    // Standard Phong
    vec3 R = reflect(-lightdir, norm);
    return pow(max(tangDot(R, viewdir), 0.0), shiny);
}

vec2 computeLighting(in vec3 wvPos, in vec3 wvNorm, in vec3 wvViewDir, in vec3 wvLightDir){
   float diffuseFactor = lightComputeDiffuse(wvNorm, wvLightDir, wvViewDir);
   float specularFactor = lightComputeSpecular(wvNorm, wvViewDir, wvLightDir, m_Shininess);
   specularFactor *= step(1.0, m_Shininess);

   float att = vLightDir.w;

   return vec2(diffuseFactor, specularFactor) * vec2(att);
}


#ifdef ALPHAMAP

  vec4 calculateDiffuseBlend(in vec2 texCoord) {
    vec4 alphaBlend   = texture2D( m_AlphaMap, texCoord.xy );
    vec4 diffuseColor = texture2D(m_DiffuseMap, texCoord * m_DiffuseMap_0_scale);
    diffuseColor *= alphaBlend.r;
    #ifdef DIFFUSEMAP_1
      vec4 diffuseColor1 = texture2D(m_DiffuseMap_1, texCoord * m_DiffuseMap_1_scale);
      diffuseColor = mix( diffuseColor, diffuseColor1, alphaBlend.g );
      #ifdef DIFFUSEMAP_2
        vec4 diffuseColor2 = texture2D(m_DiffuseMap_2, texCoord * m_DiffuseMap_2_scale);
        diffuseColor = mix( diffuseColor, diffuseColor2, alphaBlend.b );
        #ifdef DIFFUSEMAP_3
          vec4 diffuseColor3 = texture2D(m_DiffuseMap_3, texCoord * m_DiffuseMap_3_scale);
          diffuseColor = mix( diffuseColor, diffuseColor3, alphaBlend.a );
        #endif
      #endif
    #endif
    return diffuseColor;
  }

  vec3 calculateNormal(in vec2 texCoord) {
    vec3 normal = vec3(0,0,1);

    vec4 alphaBlend = texture2D( m_AlphaMap, texCoord.xy );

    #ifdef NORMALMAP
      vec4 normalHeight = texture2D(m_NormalMap, texCoord * m_DiffuseMap_0_scale);
      vec3 n = (normalHeight.xyz * vec3(2.0) - vec3(1.0));
      n.z = sqrt(1.0 - (n.x * n.x) - (n.y * n.y));
      n.y = -n.y;
      normal += n * alphaBlend.r;
    #endif

    #ifdef NORMALMAP_1
      vec4 normalHeight = texture2D(m_NormalMap_1, texCoord * m_DiffuseMap_1_scale);
      vec3 n = (normalHeight.xyz * vec3(2.0) - vec3(1.0));
      n.z = sqrt(1.0 - (n.x * n.x) - (n.y * n.y));
      n.y = -n.y;
      normal += n * alphaBlend.g;
    #endif

    #ifdef NORMALMAP_2
      vec4 normalHeight = texture2D(m_NormalMap_2, texCoord * m_DiffuseMap_2_scale);
      vec3 n = (normalHeight.xyz * vec3(2.0) - vec3(1.0));
      n.z = sqrt(1.0 - (n.x * n.x) - (n.y * n.y));
      n.y = -n.y;
      normal += n * alphaBlend.b;
    #endif

    #ifdef NORMALMAP_3
      vec4 normalHeight = texture2D(m_NormalMap_3, texCoord * m_DiffuseMap_3_scale);
      vec3 n = (normalHeight.xyz * vec3(2.0) - vec3(1.0));
      n.z = sqrt(1.0 - (n.x * n.x) - (n.y * n.y));
      n.y = -n.y;
      normal += n * alphaBlend.a;
    #endif

    return normalize(normal);
  }

  #ifdef TRI_PLANAR_MAPPING

    vec4 calculateTriPlanarDiffuseBlend(in vec3 wNorm, in vec4 wVert, in vec2 texCoord) {
        // tri-planar texture bending factor for this fragment's normal
        vec3 blending = abs( wNorm );
        blending = (blending -0.2) * 0.7;
        blending = normalize(max(blending, 0.00001));      // Force weights to sum to 1.0 (very important!)
        float b = (blending.x + blending.y + blending.z);
        blending /= vec3(b, b, b);

        // texture coords
        vec4 coords = wVert;

        vec4 col1 = texture2D( m_DiffuseMap, coords.yz * m_DiffuseMap_0_scale);
        vec4 col2 = texture2D( m_DiffuseMap, coords.xz * m_DiffuseMap_0_scale );
        vec4 col3 = texture2D( m_DiffuseMap, coords.xy * m_DiffuseMap_0_scale );
        // blend the results of the 3 planar projections.
        vec4 tex0 = col1 * blending.x + col2 * blending.y + col3 * blending.z;

        #ifdef DIFFUSEMAP_1
          col1 = texture2D( m_DiffuseMap_1, coords.yz * m_DiffuseMap_1_scale);
          col2 = texture2D( m_DiffuseMap_1, coords.xz * m_DiffuseMap_1_scale );
          col3 = texture2D( m_DiffuseMap_1, coords.xy * m_DiffuseMap_1_scale );
          vec4 tex1 = col1 * blending.x + col2 * blending.y + col3 * blending.z;
        #endif
        #ifdef DIFFUSEMAP_2
          col1 = texture2D( m_DiffuseMap_2, coords.yz * m_DiffuseMap_2_scale);
          col2 = texture2D( m_DiffuseMap_2, coords.xz * m_DiffuseMap_2_scale );
          col3 = texture2D( m_DiffuseMap_2, coords.xy * m_DiffuseMap_2_scale );
          vec4 tex2 = col1 * blending.x + col2 * blending.y + col3 * blending.z;
        #endif
        #ifdef DIFFUSEMAP_3
          col1 = texture2D( m_DiffuseMap_3, coords.yz * m_DiffuseMap_3_scale);
          col2 = texture2D( m_DiffuseMap_3, coords.xz * m_DiffuseMap_3_scale );
          col3 = texture2D( m_DiffuseMap_3, coords.xy * m_DiffuseMap_3_scale );
          vec4 tex3 = col1 * blending.x + col2 * blending.y + col3 * blending.z;
        #endif

        vec4 alphaBlend   = texture2D( m_AlphaMap, texCoord.xy );
        vec4 diffuseColor = tex0 * alphaBlend.r;
        #ifdef DIFFUSEMAP_1
          diffuseColor = mix( diffuseColor, tex1, alphaBlend.g );
          #ifdef DIFFUSEMAP_2
            diffuseColor = mix( diffuseColor, tex2, alphaBlend.b );
            #ifdef DIFFUSEMAP_3
              diffuseColor = mix( diffuseColor, tex3, alphaBlend.a );
            #endif
          #endif
        #endif

        return diffuseColor;
    }

    vec3 calculateNormalTriPlanar(in vec3 wNorm, in vec4 wVert,in vec2 texCoord) {
      // tri-planar texture bending factor for this fragment's world-space normal
      vec3 blending = abs( wNorm );
      blending = (blending -0.2) * 0.7;
      blending = normalize(max(blending, 0.00001));      // Force weights to sum to 1.0 (very important!)
      float b = (blending.x + blending.y + blending.z);
      blending /= vec3(b, b, b);

      vec3 normal = vec3(0,0,1);
      //TODO
      return normal;
    }
  #endif

#endif



void main(){

    //----------------------
    // diffuse calculations
    //----------------------
    #ifdef DIFFUSEMAP
      #ifdef ALPHAMAP
        #ifdef TRI_PLANAR_MAPPING
            vec4 diffuseColor = calculateTriPlanarDiffuseBlend(wNormal, wVertex, texCoord);
        #else
            vec4 diffuseColor = calculateDiffuseBlend(texCoord);
        #endif
      #else
        vec4 diffuseColor = texture2D(m_DiffuseMap, texCoord);
      #endif
    #else
      vec4 diffuseColor = vec4(1.0);
    #endif

    
    //---------------------
    // normal calculations
    //---------------------
    #if defined(NORMALMAP) || defined(NORMALMAP_1) || defined(NORMALMAP_2) || defined(NORMALMAP_3)
      #ifdef TRI_PLANAR_MAPPING
        vec3 normal = calculateNormalTriPlanar(wNormal, wVertex, texCoord);
      #else
        vec3 normal = calculateNormal(texCoord);
      #endif
    #else
      vec3 normal = vNormal;
    #endif


    //-----------------------
    // lighting calculations
    //-----------------------
    vec4 lightDir = vLightDir;
    lightDir.xyz = normalize(lightDir.xyz);

    vec2 light = computeLighting(vPosition, normal, vViewDir.xyz, lightDir.xyz);

    vec4 specularColor = vec4(1.0);

    //--------------------------
    // final color calculations
    //--------------------------
    gl_FragColor =  AmbientSum * diffuseColor +
                    DiffuseSum * diffuseColor  * light.x +
                    SpecularSum * specularColor * light.y;

    //gl_FragColor.a = alpha;
}