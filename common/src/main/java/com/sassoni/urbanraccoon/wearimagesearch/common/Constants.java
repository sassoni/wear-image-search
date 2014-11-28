package com.sassoni.urbanraccoon.wearimagesearch.common;

public class Constants {

    private Constants() {
    }

    public static final int MAX_IMAGES_PER_REQUEST = 10;
    public static final int MAX_IMAGES_TOTAL = 30;

    public static final String PATH_IMAGE = "/image";
    public static final String PATH_SEARCH = "/search";
    public static final String PATH_OPEN = "/open";
    public static final String PATH_ERROR = "/error";

    public static String DMAP_KEY_IMAGE = "image";
    public static String DMAP_KEY_INDEX = "index";
    public static String DMAP_KEY_SEARCH_TERM = "searchTerm";
    public static String DMAP_KEY_CONTEXT_URL = "contextUrl";
    public static String DMAP_KEY_TIME = "time";

}
