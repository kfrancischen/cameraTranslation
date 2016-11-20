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
    public void DatabaseFile(){
        int k = 0;
        for(int i = 0; i < chineseVocabulary.size(); i++){
            for(int j = 0; j < 3; j++ ) {
                vocabulary.put(englishVocabulary.get(k), chineseVocabulary.get(i));
                k += 1;
            }
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
    /* this is the dictionary for the English words
       each line should have the format: all lower letter, initial letter capitalized, all capital letter
     */
    private List<String> englishVocabulary = Arrays.asList("exit", "Exit", "EXIT",
        "entrance", "Entrance", "ENTRANCE",
        "stop", "Stop", "STOP");
    /* this is the dictionary for the corresponding Chinese words
       each line should have the Chinese translation for the corresponding lines of the englishVocabulary
     */
    private List<String> chineseVocabulary = Arrays.asList("出口",
            "入口",
            "停止");
}
