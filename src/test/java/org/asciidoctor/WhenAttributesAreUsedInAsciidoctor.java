package org.asciidoctor;

import static org.asciidoctor.AttributesBuilder.attributes;
import static org.asciidoctor.OptionsBuilder.options;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.junit.Assert.assertThat;
import static org.xmlmatchers.xpath.HasXPath.hasXPath;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.asciidoctor.internal.JRubyAsciidoctor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import com.google.common.io.CharStreams;

public class WhenAttributesAreUsedInAsciidoctor {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private Asciidoctor asciidoctor = JRubyAsciidoctor.create();

    @Test
    public void qualified_http_url_inline_with_hide_uri_scheme_set() throws IOException {
        
        Attributes attributes = attributes().hiddenUriScheme(true).get();
        
        String content = asciidoctor.render("The AsciiDoc project is located at http://asciidoc.org.", OptionsBuilder.options().attributes(attributes));

        Document doc = Jsoup.parse(content, "UTF-8");
        
        Element link = doc.getElementsByTag("a").first();
        assertThat(link.text(), is("asciidoc.org"));
        
    }
    
    @Test
    public void compat_mode_should_change_how_document_is_rendered_to_legacy_system() {
        
        Attributes attributes = attributes().attribute("version", "1.0.0").compatMode(CompatMode.LEGACY).get();
        
        String content = asciidoctor.render("The `AsciiDoc {version}` project.", OptionsBuilder.options().attributes(attributes));

        Document doc = Jsoup.parse(content, "UTF-8");
        Element code = doc.getElementsByTag("code").first();
        assertThat(code.text(), containsString("{version}"));
        
    }
    
    @Test
    public void no_compat_mode_should_change_how_document_is_rendered_to_new_system() {
        
        Attributes attributes = attributes().attribute("version", "1.0.0").get();
        
        String content = asciidoctor.render("The `AsciiDoc {version}` project.", OptionsBuilder.options().attributes(attributes));

        Document doc = Jsoup.parse(content, "UTF-8");
        Element code = doc.getElementsByTag("code").first();
        assertThat(code.text(), containsString("1.0.0"));
        
    }
    
    @Test
    public void should_preload_open_cache_uri_gem() throws IOException {
        
        Attributes attributes = attributes().cacheUri(true).get();
        
        String content = asciidoctor.render("read my lips", OptionsBuilder.options().attributes(attributes));

        assertThat(content, is(notNullValue()));
        
    }
    
    @Test
    public void should_add_AsciiMath_delimiters_around_math_block_content_if_math_attribute_not_latexmath() throws IOException {
        
        Attributes attributes = attributes().math("asciimath").get();
        
        Options options = options().inPlace(false).safe(SafeMode.UNSAFE)
                .toDir(testFolder.getRoot()).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/math.asciidoc"), options);

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "math.html"), "UTF-8");
        
        assertThat(doc.getElementsByAttributeValue("type", "text/x-mathjax-config").size(), is(1));
    }
    
    @Test
    public void should_use_custom_appendix_caption_if_specified() throws IOException {
        
        Attributes attributes = attributes().appendixCaption("App").get();
        
        Options options = options().inPlace(false).safe(SafeMode.UNSAFE)
                .toDir(testFolder.getRoot()).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/appendix.asciidoc"), options);

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "appendix.html"), "UTF-8");
        
        Element attributeElement = doc.getElementById("_attribute_options");
        assertThat(attributeElement.text(), startsWith("App"));
    }
    
    @Test
    public void should_add_a_hardbreak_at_end_of_each_line_when_hardbreaks_option_is_set() throws IOException {
        
        Attributes attributes = attributes().hardbreaks(true).get();
        
        String content = asciidoctor.render("read\nmy\nlips", OptionsBuilder.options().attributes(attributes));

        Document doc = Jsoup.parse(content, "UTF-8");
        Element paragraph = doc.getElementsByAttributeValue("class","paragraph").first();
        assertThat(paragraph.getElementsByTag("br").size(), is(2));
        
    }
    
    @Test
    public void sect_num_levels_attribute_should_only_number_levels_up_to_value_defined_by_sectnumlevels_attribute() throws IOException {
        
        Attributes attributes = attributes().sectionNumbers(true).sectNumLevels(2).get();
        
        Options options = options().inPlace(false).safe(SafeMode.UNSAFE)
                .toDir(testFolder.getRoot()).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/multiple_levels.asciidoc"), options);

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "multiple_levels.html"), "UTF-8");
        
        assertThat(doc.getElementById("_level_1").text(), startsWith("1."));
        assertThat(doc.getElementById("_level_2").text(), startsWith("1.1."));
        assertThat(doc.getElementById("_level_3").text(), not(startsWith("1.1.1.")));
        assertThat(doc.getElementById("_level_4").text(), not(startsWith("1.1.1.1.")));
    }
    
    @Test
    public void no_footer_attribute_should_not_show_footer_info() throws IOException {
        
        Attributes attributes = attributes().noFooter(true).get();
        
        Options options = options().inPlace(false).safe(SafeMode.UNSAFE)
                .toDir(testFolder.getRoot()).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "rendersample.html"), "UTF-8");
        
        assertThat(doc.getElementById("footer"), is(nullValue()));
        
    }
    
    @Test
    public void source_highlighter_attribute_should_add_required_javascript_libraries_as_highlighter() throws IOException {
        
        Attributes attributes = attributes().sourceHighlighter("prettify").get();
        
        Options options = options().inPlace(false).safe(SafeMode.UNSAFE)
                .toDir(testFolder.getRoot()).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "rendersample.html"), "UTF-8");
        Elements link = doc.select("link");
        assertThat(link.get(1).attr("href"), containsString("prettify.min.css"));
        
    }
    
    @Test
    public void render_content_without_attributes_should_embed_css_by_default() throws IOException {
        
        Options options = options().inPlace(false).safe(SafeMode.UNSAFE)
                .toDir(testFolder.getRoot()).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "rendersample.html"), "UTF-8");
        Elements cssStyle = doc.select("style");
        assertThat(cssStyle.html(), is(not("")));

        Elements link = doc.select("link");
        assertThat(link.html(), is("".trim()));
        
    }
    
    @Test
    public void should_skip_front_matter_if_specified_by_skip_front_matter_attribute()
            throws IOException {

        Attributes attributes = attributes().skipFrontMatter(true).get();
        Options options = options().toFile(false).inPlace(false).attributes(attributes).get();

        String content = asciidoctor.renderFile(new File(
                "target/test-classes/renderwithfrontmatter.adoc"), options);
        Document doc = Jsoup.parse(content, "UTF-8");
        Elements hrElements = doc.getElementsByTag("hr");

        assertThat(hrElements.size(), is(0));

    }

    @Test
    public void set_anchors_attribute_should_add_anchor_to_sections() {

        Attributes attributes = attributes().setAnchors(true).get();
        Options options = options().inPlace(false).toFile(false).attributes(attributes).get();

        String content = asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        Document doc = Jsoup.parse(content, "UTF-8");
        Element anchorElement = doc.select("a[class=anchor]").first();

        assertThat(anchorElement.attr("href"), is("#_section_a"));

    }

    @Test
    public void ignore_undefined_attributes_should_keep_lines_with_undefined_attributes() {

        Attributes attributes = attributes().ignoreUndefinedAttributes(true)
                .get();
        Options options = options().toFile(false).attributes(attributes).get();

        String renderContent = asciidoctor.renderFile(new File(
                "target/test-classes/documentwithundefinedattribute.asciidoc"),
                options);
        assertThat(renderContent, containsString("{bogus-attribute}"));
    }

    @Test
    public void setting_toc_attribute_table_of_contents_should_be_generated() throws IOException {

        Attributes attributes = attributes().tableOfContents(true).get();
        Options options = options().inPlace(false).toDir(testFolder.getRoot()).safe(SafeMode.UNSAFE).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/tocsample.asciidoc"), options);

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "tocsample.html"), "UTF-8");
        Elements tocElement = doc.select("div.toc");
        assertThat(tocElement.hasClass("toc"), is(true));

    }

    @Test
    public void attribute_missing_should_drop_line_should_drop_line_with_reference_to_missing_attribute_if_attribute_missing_attribute_is_drop_line() {

        Attributes attributes = attributes().attributeMissing("drop-line")
                .get();
        Options options = options().attributes(attributes).get();

        String renderContent = asciidoctor.render("This is\n"
                + "blah blah {foobarbaz}\n" + "all there is.", options);

        assertThat(renderContent, not(containsString("{foobarbaz}")));

    }

    @Test
    public void attribute_undefined_should_not_drop_line_with_attribute_unassignment_if_attribute_undefined_is_drop() {
        
        Attributes attributes = attributes().attributeUndefined("drop")
                .get();
        Options options = options().attributes(attributes).get();
        
        String renderContent = asciidoctor.render(":foo:\n\n{set:foo!}\n{foo}yes", options);

        Document doc = Jsoup.parse(renderContent, "UTF-8");
        Element paragraph = doc.getElementsByTag("p").first();
        assertThat(paragraph.text(), is("{foo}yes"));
    }
    
    @Test
    public void table_of_content_should_be_placeable() throws IOException {

        Attributes attributes = attributes().tableOfContents(Placement.RIGHT)
                .get();
        Options options = options().inPlace(false)
                .toFile(new File(testFolder.getRoot(), "toc2sample.html"))
                .safe(SafeMode.UNSAFE).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/toc2sample.asciidoc"), options);

        File renderedFile = new File(testFolder.getRoot(), "toc2sample.html");
        Document doc = Jsoup.parse(renderedFile, "UTF-8");
        Elements body = doc.select("body");
        String classAttribute = body.attr("class");
        String[] classAttributes = classAttribute.split(" ");
        assertThat(classAttributes, hasItemInArray("toc2"));
        assertThat(classAttributes, hasItemInArray("toc-right"));

        renderedFile.delete();
    }

    @Test
    public void table_of_content_2_should_be_placeable() throws IOException {

        Attributes attributes = attributes().tableOfContents2(Placement.RIGHT)
                .get();
        Options options = options().inPlace(false)
                .toFile(new File(testFolder.getRoot(), "toc2sample.html"))
                .safe(SafeMode.UNSAFE).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/toc2sample.asciidoc"), options);

        File renderedFile = new File(testFolder.getRoot(), "toc2sample.html");
        Document doc = Jsoup.parse(renderedFile, "UTF-8");
        Elements body = doc.select("body");
        String classAttribute = body.attr("class");
        String[] classAttributes = classAttribute.split(" ");
        assertThat(classAttributes, hasItemInArray("toc2"));
        assertThat(classAttributes, hasItemInArray("toc-right"));

        renderedFile.delete();
    }

    @Test
    public void setting_linkcss_as_false_in_string_should_embed_css_file() throws IOException {

    	Attributes attributes = attributes("linkcss!").get();
        Options options = options().inPlace(false).safe(SafeMode.UNSAFE)
                .toDir(testFolder.getRoot()).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        // String readFull = IOUtils.readFull(new FileInputStream(new
        // File(testFolder.getRoot(), "rendersample.html")));

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "rendersample.html"), "UTF-8");
        Elements cssStyle = doc.select("style");
        assertThat(cssStyle.html(), is(not("")));

        Elements link = doc.select("link");
        assertThat(link.html(), is("".trim()));

    }
    
    @Test
    public void setting_toc_attribute_and_numbered_in_string_form_table_of_contents_should_be_generated() throws IOException {

        Attributes attributes = attributes("toc numbered").get();
        Options options = options().inPlace(false).toDir(testFolder.getRoot()).safe(SafeMode.UNSAFE).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/tocsample.asciidoc"), options);

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "tocsample.html"), "UTF-8");
        Elements tocElement = doc.select("div.toc");
        assertThat(tocElement.hasClass("toc"), is(true));

        Element tocParagraph = doc.select("a[href=#_first_chapter]").first();
        assertThat(tocParagraph.text(), startsWith("1."));
    }

    @Test
    public void setting_toc_attribute_and_numbered_in_array_form_table_of_contents_should_be_generated() throws IOException {

        Attributes attributes = attributes(new String[] { "toc", "numbered" })
                .get();
        Options options = options().inPlace(false).toDir(testFolder.getRoot()).safe(SafeMode.UNSAFE).attributes(attributes).get();

       asciidoctor.renderFile(new File(
                "target/test-classes/tocsample.asciidoc"), options);

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "tocsample.html"), "UTF-8");
        Elements tocElement = doc.select("div.toc");
        assertThat(tocElement.hasClass("toc"), is(true));

        Element tocParagraph = doc.select("a[href=#_first_chapter]").first();
        assertThat(tocParagraph.text(), startsWith("1."));
    }

    @Test
    public void unsetting_toc_attribute_table_of_contents_should_not_be_generated() {

        Attributes attributes = attributes().tableOfContents(false).get();
        Options options = options().toFile(false).attributes(attributes).get();

        String renderContent = asciidoctor.renderFile(new File(
                "target/test-classes/tocsample.asciidoc"), options);

        Document doc = Jsoup.parse(renderContent, "UTF-8");
        Elements tocElement = doc.select("div.toc");
        assertThat(tocElement.hasClass("toc"), is(false));

    }

    @Test
    public void styleSheetName_is_set_custom_stylesheet_should_be_used_()
            throws IOException {

        Attributes attributes = attributes().linkCss(true)
                .styleSheetName("mycustom.css").get();
        Options options = options().inPlace(false).safe(SafeMode.UNSAFE)
                .toDir(testFolder.getRoot()).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "rendersample.html"), "UTF-8");
        Elements link = doc.select("link[href]");
        String attr = link.attr("href");
        assertThat(attr, is("./mycustom.css"));

    }

    @Test
    public void unsetting_styleSheetName_should_leave_document_without_style()
            throws IOException {

        Attributes attributes = attributes().unsetStyleSheet().get();
        Options options = options().inPlace(false).safe(SafeMode.UNSAFE)
                .toDir(testFolder.getRoot()).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "rendersample.html"), "UTF-8");
        Elements link = doc.select("link[href]");
        String attr = link.attr("href");
        assertThat(attr, is(""));

    }

    @Test
    public void styles_dir_is_set_css_routes_should_use_it() throws IOException {

        Attributes attributes = attributes().stylesDir("./styles")
                .linkCss(true).styleSheetName("mycustom.css").get();
        Options options = options().inPlace(false).safe(SafeMode.UNSAFE)
                .toDir(testFolder.getRoot()).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "rendersample.html"), "UTF-8");
        Elements link = doc.select("link[href]");
        String attr = link.attr("href");
        assertThat(attr, is("./styles/mycustom.css"));

    }

    @Test
    public void unsetting_linkcss_should_embed_css_file() throws IOException {

        Attributes attributes = attributes().linkCss(false).get();
        Options options = options().inPlace(false).safe(SafeMode.UNSAFE)
                .toDir(testFolder.getRoot()).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        // String readFull = IOUtils.readFull(new FileInputStream(new
        // File(testFolder.getRoot(), "rendersample.html")));

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "rendersample.html"), "UTF-8");
        Elements cssStyle = doc.select("style");
        assertThat(cssStyle.html(), is(not("")));

        Elements link = doc.select("link");
        assertThat(link.html(), is("".trim()));

    }

    @Test
    public void linkcss_should_not_embed_css_file() throws IOException {

        Attributes attributes = attributes().linkCss(true).get();
        Options options = options().inPlace(false).safe(SafeMode.UNSAFE)
                .toDir(testFolder.getRoot()).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        Document doc = Jsoup.parse(new File(testFolder.getRoot(),
                "rendersample.html"), "UTF-8");
        Elements link = doc.select("link[href]");
        String attr = link.get(1).attr("href");
        assertThat(attr, is("./asciidoctor.css"));

    }

    @Test
    public void copycss_with_in_place_should_copy_css_to_rendered_directory() {
        Attributes attributes = attributes().linkCss(true).copyCss(true).get();
        Options options = options().inPlace(true).safe(SafeMode.UNSAFE)
                .attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        File cssFile = new File("target/test-classes/asciidoctor.css");
        assertThat(cssFile.exists(), is(true));
        cssFile.delete();

    }

    @Test
    public void copycss_negated_with_in_place_should_not_copy_css_to_rendered_directory() {
        Attributes attributes = attributes().copyCss(false).get();
        Options options = options().inPlace(true).safe(SafeMode.UNSAFE)
                .attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        File cssFile = new File("target/test-classes/asciidoctor.css");
        assertThat(cssFile.exists(), is(false));

    }

    @Test
    public void copycss_and_linkcss_negated_should_not_copy_css_to_rendered_file() {

        Attributes attributes = attributes().copyCss(true).linkCss(false).get();
        Options options = options().inPlace(true).safe(SafeMode.UNSAFE)
                .attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        File cssFile = new File("target/test-classes/asciidoctor.css");
        assertThat(cssFile.exists(), is(false));

    }

    @Test
    public void copycss_with_to_file_should_copy_css_to_to_file_directory() {

        Attributes attributes = attributes().linkCss(true).copyCss(true).get();
        Options options = options().inPlace(false)
                .toFile(new File(testFolder.getRoot(), "output.html"))
                .safe(SafeMode.UNSAFE).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        File cssFile = new File(testFolder.getRoot(), "asciidoctor.css");
        assertThat(cssFile.exists(), is(true));

    }

    @Test
    public void copycss_with_to_dir_should_copy_css_to_to_dir_directory() {

        Attributes attributes = attributes().linkCss(true).copyCss(true).get();
        Options options = options().inPlace(false).toDir(testFolder.getRoot())
                .safe(SafeMode.UNSAFE).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/rendersample.asciidoc"), options);

        File cssFile = new File(testFolder.getRoot(), "asciidoctor.css");
        assertThat(cssFile.exists(), is(true));

    }

    @Test
    public void copycss_with_render_to_file_should_copy_css_to_to_file_directory() {

        Attributes attributes = attributes().linkCss(true).copyCss(true).get();
        Options options = options().inPlace(false)
                .toFile(new File(testFolder.getRoot(), "output.html"))
                .safe(SafeMode.UNSAFE).attributes(attributes).get();

        asciidoctor.render("This is Asciidoctor", options);

        File cssFile = new File(testFolder.getRoot(), "asciidoctor.css");
        assertThat(cssFile.exists(), is(true));

    }

    @Test
    public void copycss_with_render_to_dir_should_copy_css_to_to_dir_directory() {

        Attributes attributes = attributes().linkCss(true).copyCss(true).get();
        Options options = options().inPlace(false).toDir(testFolder.getRoot())
                .safe(SafeMode.UNSAFE).attributes(attributes).get();

        asciidoctor.render("This is Asciidoctor", options);

        File cssFile = new File(testFolder.getRoot(), "asciidoctor.css");
        assertThat(cssFile.exists(), is(true));

    }

    @Test
    public void string_content_with_icons_enabled_should_be_rendered()
            throws IOException, SAXException, ParserConfigurationException {

        InputStream content = new FileInputStream(
                "target/test-classes/documentwithnote.asciidoc");

        Map<String, Object> attributes = attributes().icons(
                Attributes.IMAGE_ICONS).asMap();
        Map<String, Object> options = options().attributes(attributes).asMap();

        String result = asciidoctor.render(toString(content), options);
        result = result.replaceAll("<img(.*?)>", "<img$1/>");
        assertRenderedAdmonitionIcon(result);

    }

    @Test
    public void string_content_with_fontawesome_icons_enabled_should_be_rendered()
            throws IOException, SAXException, ParserConfigurationException {

        InputStream content = new FileInputStream(
                "target/test-classes/documentwithnote.asciidoc");

        Map<String, Object> attributes = attributes().icons(
                Attributes.FONT_ICONS).asMap();
        Map<String, Object> options = options().attributes(attributes).asMap();

        String result = asciidoctor.render(toString(content), options);
        assertRenderedFontAwesomeAdmonitionIcon(result);

    }

    @Test
    public void string_content_with_icons_enabled_and_iconsdir_set_should_be_rendered_with_iconsdir()
            throws IOException, SAXException, ParserConfigurationException {

        InputStream content = new FileInputStream(
                "target/test-classes/documentwithnote.asciidoc");

        Map<String, Object> attributes = attributes()
                .icons(Attributes.IMAGE_ICONS).iconsDir("icons").asMap();
        Map<String, Object> options = options().attributes(attributes).asMap();

        String renderContent = asciidoctor.render(toString(content), options);

        Document doc = Jsoup.parse(renderContent, "UTF-8");
        Elements image = doc.select("img[src]");
        String srcValue = image.attr("src");
        assertThat(srcValue, is("icons/note.png"));

    }

    @Test
    public void linkattrs_should_make_asciidoctor_render_link_macro_attributes() {

        Attributes attributes = attributes().linkAttrs(true).get();
        Options options = options().attributes(attributes).get();

        String content = asciidoctor.render(
                "http://google.com[Google, window=\"_blank\"]", options);

        Document doc = Jsoup.parse(content);
        Elements image = doc.select("a[target]");

        String targetValue = image.attr("target");
        assertThat(targetValue, is("_blank"));

    }

    @Test
    public void experimental_flag_should_enable_experimental_features_like_keyboard_shortcuts() {

        Attributes attributes = attributes().experimental(true).get();
        Options options = options().attributes(attributes).get();

        String content = asciidoctor.render("kbd:[F11]", options);

        Document doc = Jsoup.parse(content);
        Elements image = doc.select("kbd");

        assertThat(image.text(), is("F11"));
    }

    @Ignore
    // Igonre because of fail only in Travis and cannot inspect the report.
    @Test
    public void iconfont_attributes_should_be_used_for_using_custom_font_css_icons()
            throws URISyntaxException, IOException {

        Attributes attributes = attributes().icons(Attributes.FONT_ICONS)
                .iconFontRemote(true).iconFontCdn(new URI("http://mycdn"))
                .iconFontName("myfont").get();
        Options options = options().inPlace(true).attributes(attributes).get();

        asciidoctor.renderFile(new File(
                "target/test-classes/documentwithnote.asciidoc"), options);

        File expectedFile = new File(
                "target/test-classes/documentwithnote.html");
        Document doc = Jsoup.parse(expectedFile, "UTF-8");

        Elements stylesheetLinks = doc
                .select("link[href$=http://mycdn/myfont.min.css]");
        assertThat(stylesheetLinks.size(), is(1));

        expectedFile.delete();

    }

    private void assertRenderedFontAwesomeAdmonitionIcon(String renderContent)
            throws IOException, SAXException, ParserConfigurationException {

        Source renderFileSource = new DOMSource(
                inputStream2Document(new ByteArrayInputStream(
                        renderContent.getBytes())));
        assertThat(renderFileSource, hasXPath("//i[@class='fa icon-note']"));
    }

    private void assertRenderedAdmonitionIcon(String render_content)
            throws IOException, SAXException, ParserConfigurationException {
        Source renderFileSource = new DOMSource(
                inputStream2Document(new ByteArrayInputStream(
                        render_content.getBytes())));

        assertThat(renderFileSource, hasXPath("//img[@alt='Note']"));
    }

    private static String toString(InputStream inputStream) throws IOException {
        return CharStreams.toString(new InputStreamReader(inputStream));
    }

    private static org.w3c.dom.Document inputStream2Document(
            InputStream inputStream) throws IOException, SAXException,
            ParserConfigurationException {
        DocumentBuilderFactory newInstance = DocumentBuilderFactory
                .newInstance();
        newInstance.setNamespaceAware(true);
        org.w3c.dom.Document parse = newInstance.newDocumentBuilder().parse(
                inputStream);
        return parse;
    }

}
