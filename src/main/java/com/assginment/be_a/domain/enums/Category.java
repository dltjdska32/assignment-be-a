package com.assginment.be_a.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public enum Category {

    ///  최상위 카테고리 (parent == null, level == 1)
    DEVELOPMENT("개발", null, 1L),
    LANGUAGE("외국어", null, 1L),
    DESIGN("디자인", null, 1L),
    CERTIFICATE("자격증", null, 1L),
    UNIVERSITY("대학 교육", null, 1L),
    ETC("기타", null, 1L),

    ///  개발 하위 (level == 2)
    WEB_DEVELOPMENT("웹개발", DEVELOPMENT, 2L),
    FRONTEND("프론트엔드", DEVELOPMENT, 2L),
    BACKEND("백엔드", DEVELOPMENT, 2L),
    FULLSTACK("풀스택", DEVELOPMENT, 2L),
    APP_DEVELOPMENT("앱 개발", DEVELOPMENT, 2L),
    DATABASE("데이터베이스", DEVELOPMENT, 2L),

    ///  외국어 하위
    KOREAN("한국어", LANGUAGE, 2L),
    POLISH("폴란드어", LANGUAGE, 2L),
    ENGLISH("영어", LANGUAGE, 2L),
    JAPANESE("일본어", LANGUAGE, 2L),
    GERMAN("독일어", LANGUAGE, 2L),
    SPANISH("스페인어", LANGUAGE, 2L),
    CHINESE("중국어", LANGUAGE, 2L),

    ///  디자인 하위
    CAD("CAD", DESIGN, 2L),
    GRAPHIC_DESIGN("그래픽 디자인", DESIGN, 2L),
    PHOTO("사진", DESIGN, 2L),
    VIDEO("영상", DESIGN, 2L),

    ///  자격증 하위
    INFO_PROCESSING_ENGINEER("정보처리기사", CERTIFICATE, 2L),
    INFO_SECURITY_ENGINEER("정보보안기사", CERTIFICATE, 2L),
    SQLD("SQLD", CERTIFICATE, 2L),
    COMPUTER_LITERACY_LV1("컴퓨터활용능력 1급", CERTIFICATE, 2L),
    COMPUTER_LITERACY_LV2("컴퓨터활용능력 2급", CERTIFICATE, 2L),
    COMPUTER_LITERACY_LV3("컴퓨터활용능력 3급", CERTIFICATE, 2L),

    ///  대학 교육 하위
    MATH("수학", UNIVERSITY, 2L),
    ENGINEERING("공학", UNIVERSITY, 2L),
    NATURAL_SCIENCE("자연과학", UNIVERSITY, 2L),
    EDUCATION("교육학", UNIVERSITY, 2L);


    private final String label;
    private final Category parent;
    private final Long level;


    public boolean isRoot() {
        return parent == null;
    }

    public static List<Category> all() {
        return List.of(values());
    }
}
