/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.lang.css.s3;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
interface S3Syntax {
	char SYM_ATTRIB_BEGIN = '[';
	char SYM_ATTRIB_END = ']';
	char SYM_CALL = ':';
	char SYM_CLASS = '.';
	char SYM_CLOSED_LEFT = '(';
	char SYM_CLOSED_RIGHT = ')';
	char SYM_DIRECT_CHILD = '>';
	char SYM_LINE = ';';
	char SYM_QUOTE1 = '\"';
	char SYM_QUOTE2 = '\'';
	char SYM_QUOTE_ESC = '\\';
	char SYM_REF = '#';
	char SYM_SELF = '&';
	char SYM_SET = ':';
	char SYM_STRUCTURE_LEFT = '{';
	char SYM_STRUCTURE_RIGHT = '}';
	char SYM_VAR = '$';
	char SYM_WILD = '@';
}