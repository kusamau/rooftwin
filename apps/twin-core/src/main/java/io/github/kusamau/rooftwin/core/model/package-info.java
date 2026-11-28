/**
 * Java mirror of the DTDL v3 information model in {@code models/dtdl/}.
 *
 * <p>{@link io.github.kusamau.rooftwin.core.model.Telemetry} and {@link
 * io.github.kusamau.rooftwin.core.model.Command} are sealed interfaces with one record per DTDL
 * interface or command; {@link io.github.kusamau.rooftwin.core.model.Site} mirrors the root of the
 * twin graph. All of it lives in one package because, without JPMS modules, a sealed hierarchy
 * can't span packages.
 *
 * <p>The DTDL files are the source of truth. {@code DtdlConformanceTest} reads them and fails the
 * build when a record and its interface disagree.
 */
package io.github.kusamau.rooftwin.core.model;
