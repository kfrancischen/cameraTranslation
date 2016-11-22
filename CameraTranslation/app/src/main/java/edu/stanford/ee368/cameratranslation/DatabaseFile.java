package edu.stanford.ee368.cameratranslation;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

/**
 * Created by francischen on 11/20/16.
 */

public class DatabaseFile {
    private  HashMap<String, String> vocabulary = new HashMap();
    public DatabaseFile(){
        for(int i = 0; i < chineseVocabulary.size(); i++){
                vocabulary.put(englishVocabulary.get(i), chineseVocabulary.get(i));
        }
    }

    public boolean hasThisWord(String word){
        return vocabulary.containsKey(word);
    }

    public List<String> regexMatching(String pattern){
        // one needs to implement regular expression matching here
        // TODO, regular expression match searching needs be implmeneted
        return null;
    }

    // TODO, data based needs to be implemented
    /** this is the dictionary for the English words
       each line should have the format: all lower letter
       any input with upper case will finally be converted to lower case
     **/
    private List<String> englishVocabulary = Arrays.asList("exit",
        "entrance",
        "stop");
    /** this is the dictionary for the corresponding Chinese words
       each line should have the Chinese translation for the corresponding lines of the englishVocabulary
     **/
    private List<String> chineseVocabulary = Arrays.asList("出口",
            "入口",
            "停止");
}
