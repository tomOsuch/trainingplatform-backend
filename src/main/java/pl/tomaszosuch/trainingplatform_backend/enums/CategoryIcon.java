package pl.tomaszosuch.trainingplatform_backend.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum CategoryIcon {

    DUMBBELL("dumbbell"),
    FOOTPRINTS("footprints"),
    VOLLEYBALL("volleyball"),
    TROPHY("trophy"),
    BIKE("bike"),
    WAVES("waves"),
    HEART_PULSE("heart-pulse"),
    ACTIVITY("activity"),
    FLAME("flame"),
    MOUNTAIN("mountain"),
    MUSIC("music"),
    TARGET("target"),
    TIMER("timer"),
    MEDAL("medal"),
    ZAP("zap"),
    PERSON_STANDING("person-standing");

    public static final CategoryIcon DEFAULT = DUMBBELL;

    private static final Map<String, CategoryIcon> BY_VALUE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(CategoryIcon::value, icon -> icon));

    private final String value;

    CategoryIcon(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static boolean isAllowed(String value) {
        return value != null && BY_VALUE.containsKey(value);
    }

    public static String allowedAsText() {
        return Arrays.stream(values()).map(CategoryIcon::value).collect(Collectors.joining(", "));
    }
}
