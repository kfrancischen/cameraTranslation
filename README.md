# __Instranslation__
This is the repository for CS232 project: instant camera translation and voicing for signs

Author: Kaifeng Chen, <kfrancischen@gmail.com>.

### Dependencies:
1. OpenCV 3.1.0
2. Android device with Android version >= 7.0

Please refer to build.gradle for more details of the required SDK and NDK.

### Algorithm Options:
1. Algorithm 1: using Google textrecognizer service.
2. Algorithm 2: combining MSER detector with Google textrecognizer service.
3. Algorithm 3: using principle component analysis for image matching.
4. Algorithm 4: combining FAST feature detector and ORB feature matcher.

### How to use:
The search button on the left corner of the screen will process the input frame and the voice button on the right corner of the screen will speak out the recognized text. The algorithm radio buttons are for user to choose. All translations and voices are in Chinese.

### Contribute:
The data base file can be found in _DatabaseFile.java_. Concretely
```java
/** this is the dictionary for the English words
       each line should have the format: all lower letter
       any input with upper case will finally be converted to lower case
     **/
  private List<String> englishVocabulary = Arrays.asList(
            "exit",
            "entrance",
            "stop",
            "arrivals",
            "departures",
            "baggage",
            "check in",
            "information",
            "lost found",
            "money exchange",
            "restroom",
            "security",
            "custom");
    /** this is the dictionary for the corresponding Chinese words
       each line should have the Chinese translation for the corresponding lines of the englishVocabulary
     **/
    private List<String> chineseVocabulary = Arrays.asList(
            "出口",
            "入口",
            "停止",
            "到达层",
            "出发层",
            "行李",
            "登记",
            "信息咨询",
            "失物招领",
            "货币兑换",
            "卫生间",
            "安检",
            "海关");
```

One can enrich the vocabulary by adding more images to _raw_ resource folder.

Last updated: 2016.11.30.
