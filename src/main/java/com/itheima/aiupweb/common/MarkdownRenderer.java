package com.itheima.aiupweb.common;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class MarkdownRenderer {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();
    private final Safelist safelist = Safelist.relaxed()
            .addTags("pre", "code", "table", "thead", "tbody", "tr", "th", "td", "del", "input")
            .addAttributes("code", "class")
            .addAttributes("th", "align")
            .addAttributes("td", "align")
            .addAttributes("input", "type", "checked", "disabled")
            .addAttributes("a", "href", "title", "rel")
            .addAttributes("img", "src", "alt", "title")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https");

    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String html = this.renderer.render(this.parser.parse(markdown));
        String cleaned = Jsoup.clean(html, "", this.safelist,
                new Document.OutputSettings().prettyPrint(false));
        Document document = Jsoup.parseBodyFragment(cleaned);
        for (Element link : document.select("a[href]")) {
            link.attr("rel", "nofollow noopener noreferrer");
        }
        for (Element checkbox : document.select("input[type=checkbox]")) {
            checkbox.attr("disabled", "disabled");
        }
        document.outputSettings().prettyPrint(false);
        return document.body().html();
    }
}
