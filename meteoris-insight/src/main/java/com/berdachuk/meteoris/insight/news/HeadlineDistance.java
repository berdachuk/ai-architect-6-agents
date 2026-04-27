package com.berdachuk.meteoris.insight.news;

/** Headline text with L2 distance from a query vector (pgvector {@code <->} operator). */
public record HeadlineDistance(String headline, double distance) {}
