/**
 * fx_surface_face_enum.java
 *
 * This file was generated by XMLSpy 2007sp2 Enterprise Edition.
 *
 * YOU SHOULD NOT MODIFY THIS FILE, BECAUSE IT WILL BE
 * OVERWRITTEN WHEN YOU RE-RUN CODE GENERATION.
 *
 * Refer to the XMLSpy Documentation for further details.
 * http://www.altova.com/xmlspy
 */


package com.jmex.model.collada.schema;

import com.jmex.xml.types.SchemaString;

public class fx_surface_face_enum extends SchemaString {
	public static final int EPOSITIVE_X = 0; /* POSITIVE_X */
	public static final int ENEGATIVE_X = 1; /* NEGATIVE_X */
	public static final int EPOSITIVE_Y = 2; /* POSITIVE_Y */
	public static final int ENEGATIVE_Y = 3; /* NEGATIVE_Y */
	public static final int EPOSITIVE_Z = 4; /* POSITIVE_Z */
	public static final int ENEGATIVE_Z = 5; /* NEGATIVE_Z */

	public static String[] sEnumValues = {
		"POSITIVE_X",
		"NEGATIVE_X",
		"POSITIVE_Y",
		"NEGATIVE_Y",
		"POSITIVE_Z",
		"NEGATIVE_Z",
	};

	public fx_surface_face_enum() {
		super();
	}

	public fx_surface_face_enum(String newValue) {
		super(newValue);
		validate();
	}

	public fx_surface_face_enum(SchemaString newValue) {
		super(newValue);
		validate();
	}

	public static int getEnumerationCount() {
		return sEnumValues.length;
	}

	public static String getEnumerationValue(int index) {
		return sEnumValues[index];
	}

	public static boolean isValidEnumerationValue(String val) {
		for (int i = 0; i < sEnumValues.length; i++) {
			if (val.equals(sEnumValues[i]))
				return true;
		}
		return false;
	}

	public void validate() {

		if (!isValidEnumerationValue(toString()))
			throw new com.jmex.xml.xml.XmlException("Value of fx_surface_face_enum is invalid.");
	}
}