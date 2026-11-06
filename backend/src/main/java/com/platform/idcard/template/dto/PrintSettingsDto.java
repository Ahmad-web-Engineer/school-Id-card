package com.platform.idcard.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Prepress and imposition print configuration for bulk ID card printing")
public record PrintSettingsDto(
        @Schema(description = "Printing mode: SHEET (multi-card imposition) or PLASTIC_CARD (direct single PVC card printing)", example = "SHEET")
        String printMode,

        @Schema(description = "Paper standard: A4, A3, LETTER, or CR80", example = "A4")
        String paperSize,

        @Schema(description = "Print sides: DUPLEX (front & back mirrored), FRONT_ONLY, or BACK_ONLY", example = "DUPLEX")
        String sides,

        @Schema(description = "Number of card columns on sheet", example = "3")
        Integer columns,

        @Schema(description = "Number of card rows on sheet", example = "3")
        Integer rows,

        @Schema(description = "Horizontal spacing/gap between card columns in millimeters", example = "2.0")
        Double horizontalSpacingMm,

        @Schema(description = "Vertical spacing/gap between card rows in millimeters", example = "2.0")
        Double verticalSpacingMm,

        @Schema(description = "Top page padding/offset in millimeters", example = "10.0")
        Double topOffsetMm,

        @Schema(description = "Bottom page padding/offset in millimeters", example = "10.0")
        Double bottomOffsetMm,

        @Schema(description = "Left page padding/offset in millimeters", example = "10.0")
        Double leftOffsetMm,

        @Schema(description = "Right page padding/offset in millimeters", example = "10.0")
        Double rightOffsetMm,

        @Schema(description = "Whether to auto-center cards on the sheet", example = "true")
        Boolean autoCenter,

        @Schema(description = "Whether to draw crop/cutting marks around card corners", example = "true")
        Boolean showCropMarks,

        @Schema(description = "Whether to draw subtle cutting guides between cards", example = "true")
        Boolean showCutGuides,

        @Schema(description = "Admin flag: whether Printing Agency is authorized to customize print settings", example = "false")
        Boolean allowAgencyOverride
) {
    public static final String MODE_SHEET = "SHEET";
    public static final String MODE_PLASTIC = "PLASTIC_CARD";

    public static final String PAPER_A4 = "A4";
    public static final String PAPER_A3 = "A3";
    public static final String PAPER_LETTER = "LETTER";
    public static final String PAPER_CR80 = "CR80";

    public static final String SIDES_DUPLEX = "DUPLEX";
    public static final String SIDES_FRONT_ONLY = "FRONT_ONLY";
    public static final String SIDES_BACK_ONLY = "BACK_ONLY";

    /**
     * Creates standard optimal default settings for a card orientation.
     */
    public static PrintSettingsDto defaultSettings(boolean isPortrait) {
        return new PrintSettingsDto(
                MODE_SHEET,
                PAPER_A4,
                SIDES_DUPLEX,
                isPortrait ? 3 : 2,
                isPortrait ? 3 : 4,
                2.0, // 2mm gap between cards
                2.0, // 2mm gap between rows
                10.0,
                10.0,
                10.0,
                10.0,
                true, // auto-center
                true, // show crop marks
                true, // show cut guides
                false // allow agency override default false
        );
    }

    /**
     * Sanitizes settings to ensure valid boundaries and non-null values.
     */
    public PrintSettingsDto sanitize(boolean isPortrait) {
        String pMode = (printMode != null && printMode.equalsIgnoreCase(MODE_PLASTIC)) ? MODE_PLASTIC : MODE_SHEET;
        String pPaper = paperSize != null && !paperSize.isBlank() ? paperSize.toUpperCase() : PAPER_A4;
        String pSides = sides != null && !sides.isBlank() ? sides.toUpperCase() : SIDES_DUPLEX;

        int cols = columns != null && columns > 0 ? Math.min(columns, 8) : (isPortrait ? 3 : 2);
        int r = rows != null && rows > 0 ? Math.min(rows, 10) : (isPortrait ? 3 : 4);

        if (MODE_PLASTIC.equals(pMode)) {
            cols = 1;
            r = 1;
            pPaper = PAPER_CR80;
        }

        double hGap = horizontalSpacingMm != null && horizontalSpacingMm >= 0 ? Math.min(horizontalSpacingMm, 50.0) : 2.0;
        double vGap = verticalSpacingMm != null && verticalSpacingMm >= 0 ? Math.min(verticalSpacingMm, 50.0) : 2.0;

        double mTop = topOffsetMm != null && topOffsetMm >= 0 ? topOffsetMm : 10.0;
        double mBottom = bottomOffsetMm != null && bottomOffsetMm >= 0 ? bottomOffsetMm : 10.0;
        double mLeft = leftOffsetMm != null && leftOffsetMm >= 0 ? leftOffsetMm : 10.0;
        double mRight = rightOffsetMm != null && rightOffsetMm >= 0 ? rightOffsetMm : 10.0;

        boolean centered = autoCenter != null ? autoCenter : true;
        boolean crop = showCropMarks != null ? showCropMarks : true;
        boolean guides = showCutGuides != null ? showCutGuides : true;
        boolean agencyOverride = allowAgencyOverride != null ? allowAgencyOverride : false;

        return new PrintSettingsDto(
                pMode, pPaper, pSides, cols, r,
                hGap, vGap, mTop, mBottom, mLeft, mRight,
                centered, crop, guides, agencyOverride
        );
    }

    // Alias methods for clean backward-compatible callers
    public Double marginTopMm() { return topOffsetMm; }
    public Double marginBottomMm() { return bottomOffsetMm; }
    public Double marginLeftMm() { return leftOffsetMm; }
    public Double marginRightMm() { return rightOffsetMm; }
}
