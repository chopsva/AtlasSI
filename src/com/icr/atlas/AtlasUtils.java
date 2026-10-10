package com.icr.atlas;

import com.icr.atlas.ATLASGuiExtend.CommState;

public final class AtlasUtils {

	private AtlasUtils() {
		throw new UnsupportedOperationException("this is a Utility Class");
	}
	
	public static String getXML(String xml, String tag) {
		String result = "";
		String startTag = "<" + tag + ">";
		String endTag = "</" + tag + ">";

		int start = xml.indexOf(startTag);
		int end = xml.indexOf(endTag);

		if (start != -1 && end != -1 && end > start) {
			result = xml.substring(start + startTag.length(), end);
		}

		return result;
	}

	
}
