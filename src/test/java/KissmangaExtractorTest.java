import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

public class KissmangaExtractorTest {
    static final String[] mangaUrls = {"https://kissmanga.org/manga/kimi_no_tsuku_uso_to_hontou", "https://kissmanga.org/manga/kxqh9261558062112", "https://kissmanga.org/manga/read_nisekoi_manga_online_for_free2"};
    static final String[] titles = {"Kimi No Tsuku Uso To Hontou", "Attack On Titan", "Nisekoi"};
    static final String[] authors = {"Ajimine Sakufu", "Isayama Hajime", "Komi Naoshi"};
    static final String[] chapterUrls = {"https://kissmanga.org/chapter/kimi_no_tsuku_uso_to_hontou/chapter_1", "https://kissmanga.org/chapter/kxqh9261558062112/chapter_1", "https://kissmanga.org/chapter/read_nisekoi_manga_online_for_free2/chapter_1"};
    static final String[] chapterNames = {"Vol.1 Chapter 1", "Vol.1 Chapter 1: To You, 2,000 Years From Now", "Chapter 1"};

    @ParameterizedTest
    @MethodSource("urlTitles")
    public void testGetTitle(String url, String expected) {
        KissmangaExtractor kex = new KissmangaExtractor(page(url));
        Assertions.assertEquals(expected, kex.getTitle());
    }

    private static Stream<Arguments> urlTitles() {
        Stream<Arguments> str = Stream.empty();
        for (int i = 0; i < mangaUrls.length; i++) {
            str = Stream.concat(str, Stream.of(Arguments.of(mangaUrls[i], titles[i])));
        }
        return str;
    }

    @ParameterizedTest
    @MethodSource("urlAuthors")
    public void testGetAuthors(String url, String expected) {
        KissmangaExtractor kex = new KissmangaExtractor(page(url));
        Assertions.assertEquals(expected, kex.getAuthors());
    }

    private static Stream<Arguments> urlAuthors() {
        Stream<Arguments> str = Stream.empty();
        for (int i = 0; i < mangaUrls.length; i++) {
            str = Stream.concat(str, Stream.of(Arguments.of(mangaUrls[i], authors[i])));
        }
        return str;
    }

    @ParameterizedTest
    @MethodSource("urlChapters")
    public void testChapterUrls(String url, String expected) {
        KissmangaExtractor kex = new KissmangaExtractor(page(url));
        Assertions.assertEquals(expected, kex.getChapterUrls().get(0));
    }

    private static Stream<Arguments> urlChapters() {
        Stream<Arguments> str = Stream.empty();
        for (int i = 0; i < mangaUrls.length; i++) {
            str = Stream.concat(str, Stream.of(Arguments.of(mangaUrls[i], chapterUrls[i])));
        }
        return str;
    }

    @ParameterizedTest
    @MethodSource("urlNames")
    public void testChapterNames(String url, String expected) {
        KissmangaExtractor kex = new KissmangaExtractor(page(url));
        Assertions.assertEquals(expected, kex.getChapterNames().get(0));
    }

    private static Stream<Arguments> urlNames() {
        Stream<Arguments> str = Stream.empty();
        for (int i = 0; i < mangaUrls.length; i++) {
            str = Stream.concat(str, Stream.of(Arguments.of(mangaUrls[i], chapterNames[i])));
        }
        return str;
    }

    private Document page(String url) {
        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }
}

