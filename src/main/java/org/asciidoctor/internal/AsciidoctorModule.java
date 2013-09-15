package org.asciidoctor.internal;

import java.util.Map;


public interface AsciidoctorModule {

	Object render(String content, Map<String, Object> options);
	Object render_file(String filename, Map<String, Object> options);
	Object render_file_extension(String filename, String extensionName, Map<String, Object> options);
	
	Document load_file(String filename, Map<String, Object> options);
	Document load(String content, Map<String, Object> options);
}
