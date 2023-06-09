/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * The Apereo Foundation licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
*/
package org.unitime.timetable.webutil.timegrid;

import java.awt.Color;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.unitime.timetable.form.ExamGridForm;
import org.unitime.timetable.model.ExamPeriod;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.solver.exam.ExamSolverProxy;
import org.unitime.timetable.util.PdfEventHandler;
import org.unitime.timetable.util.PdfFont;
import org.unitime.timetable.webutil.timegrid.ExamGridTable.ExamGridModel.ExamGridCell;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

/**
 * @author Tomas Muller
 */
public class PdfExamGridTable extends ExamGridTable {
    private Document iDocument = null;
    private PdfPTable iPdfTable = null;

    public PdfExamGridTable(ExamGridForm form, SessionContext context, ExamSolverProxy solver) throws Exception {
        super(form, context, solver);
    }
    
    public void export(OutputStream out) throws Exception {
        int nrCols = getNrColumns();
        iDocument = (iForm.getDispMode()==DispMode.InRowHorizontal.ordinal() || iForm.getDispMode()==DispMode.InRowVertical.ordinal() ?
            new Document(new Rectangle(Math.max(PageSize.LETTER.getWidth(),60.0f+100.0f*nrCols),Math.max(PageSize.LETTER.getHeight(),60.0f+150f*nrCols)).rotate(), 30, 30, 30, 30)
        :
            new Document(new Rectangle(Math.max(PageSize.LETTER.getWidth(),60.0f+100.0f*nrCols),Math.max(PageSize.LETTER.getHeight(),60.0f+150f*nrCols)), 30, 30, 30, 30));

        PdfEventHandler.initFooter(iDocument, out);
        iDocument.open();
        
        printTable();
    
        printLegend();
    
        iDocument.close();
    }
    
    public int getNrColumns() {
        if (iForm.getDispMode()==DispMode.InRowHorizontal.ordinal()) {
            return 1 + days().size() * slots().size();
        } else if (iForm.getDispMode()==DispMode.InRowVertical.ordinal()) {
            return 1 + models().size();
        } else if (iForm.getDispMode()==DispMode.PerDayHorizontal.ordinal()) {
            return 1 + slots().size();
        } else if (iForm.getDispMode()==DispMode.PerDayVertical.ordinal()) {
            return 1 + days().size();
        } else if (iForm.getDispMode()==DispMode.PerWeekHorizontal.ordinal()) {
            return 1 + weeks().size() * slots().size();
        } else if (iForm.getDispMode()==DispMode.PerWeekVertical.ordinal()) {
            return 1 + daysOfWeek().size();
        }
        return 0;
    }
    
    private static Color sBorderColor = new Color(100,100,100);
    
    public PdfPCell createCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(sBorderColor);
        cell.setPadding(3);
        cell.setBorderWidth(0);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderWidthTop(1);
        cell.setBorderWidthBottom(1);
        cell.setBorderWidthLeft(1);
        cell.setBorderWidthRight(1);
        return cell;
    }
    
    public PdfPCell createCellNoBorder() {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(sBorderColor);
        cell.setPadding(3);
        cell.setBorderWidth(0);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    public void addText(PdfPCell cell, String text) {
        if (text==null) return;
        addText(cell, text, false);
    }
    
    public void addText(PdfPCell cell, String text, boolean bold) {
        if (text==null) return;
        if (text.indexOf("<span")>=0)
            text = text.replaceAll("</span>","").replaceAll("<span .*>", "");
        text = text.replaceAll("<br>", "\n");
        text = text.replaceAll("<BR>", "\n");
        if (cell.getPhrase()==null) {
            cell.setPhrase(new Paragraph(text, PdfFont.getFont(bold)));
            cell.setVerticalAlignment(Element.ALIGN_TOP);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        } else {
            cell.getPhrase().add(new Chunk("\n"+text, PdfFont.getFont(bold)));
        }
    }
    
    
    public void printHeaderCell(String name, boolean vertical, boolean eod, boolean eol) {
        PdfPCell c = createCell();
        if (!eol && !eod) c.setBorderWidthRight(0);
        if (name==null) {
            c.setBorderWidthLeft(0);
            c.setBorderWidthTop(0);
            c.setBorderWidthBottom(0);
        } else {
            addText(c, name);
        }
        iPdfTable.addCell(c);
    }
    
    public void printHeader(String name) throws Exception {
        boolean vertical = isVertical();
        printHeaderCell(name, vertical, false, false);
        TreeSet<Integer> days = days(), slots = slots(), weeks = weeks(), daysOfWeek = daysOfWeek();
        if (iForm.getDispMode()==DispMode.InRowHorizontal.ordinal()) {
            for (Integer day : days) {
                for (Integer slot : slots()) {
                    boolean eod = (slot.equals(slots.last()));
                    boolean eol = (eod && day.equals(days.last()));
                    printHeaderCell(getDayName(day)+"<br>"+getSlotName(slot), vertical, eod, eol);
                }
            }
        } else if (iForm.getDispMode()==DispMode.InRowVertical.ordinal()) {
            for (ExamGridModel m : models()) {
                boolean eol = m.equals(models().lastElement());
                printHeaderCell(m.getName()+(m.getSize()>0?" ("+m.getSize()+")":""), vertical, false, eol);
            }
        } else if (iForm.getDispMode()==DispMode.PerDayHorizontal.ordinal()) {
            for (Integer slot : slots()) {
                boolean eol = (slot.equals(slots.last()));
                printHeaderCell(getSlotName(slot)+"<br> ", vertical, false, eol);
            }
        } else if (iForm.getDispMode()==DispMode.PerDayVertical.ordinal()) {
            for (Integer day : days) {
                boolean eol = (day.equals(days.last()));
                printHeaderCell(getDayName(day)+"<br> ", vertical, false, eol);
            }
        } else if (iForm.getDispMode()==DispMode.PerWeekHorizontal.ordinal()) {
            for (Integer week : weeks) {
                for (Integer slot : slots) {
                    boolean eod = (slot.equals(slots.last()));
                    boolean eol = eod && (week.equals(weeks.last()));
                    printHeaderCell(getWeekName(week)+"<br>"+getSlotName(slot), vertical, eod, eol);
                }
            }
        } else if (iForm.getDispMode()==DispMode.PerWeekVertical.ordinal()) {
            for (Integer dow : daysOfWeek) {
                boolean eol = (dow.equals(daysOfWeek.last()));
                printHeaderCell(getDayOfWeekName(dow)+"<br> ", vertical, false, eol);
            }
        }
        iPdfTable.setHeaderRows(1);
    }
    
    private Color getColor(String rgbColor) {
        StringTokenizer x = new StringTokenizer(rgbColor.substring("rgb(".length(),rgbColor.length()-")".length()),",");
        return new Color(
                Integer.parseInt(x.nextToken()),
                Integer.parseInt(x.nextToken()),
                Integer.parseInt(x.nextToken()));
    }
    
    private void createTable(boolean keepTogether) {
        iPdfTable = new PdfPTable(getNrColumns());
        iPdfTable.setWidthPercentage(100);
        iPdfTable.getDefaultCell().setPadding(3);
        iPdfTable.getDefaultCell().setBorderWidth(1);
        iPdfTable.setSplitRows(false);
        iPdfTable.setSpacingBefore(10);
        iPdfTable.setKeepTogether(keepTogether);
    }
    
    private void flushTable() throws Exception {
        if (iPdfTable!=null)
            iDocument.add(iPdfTable);
        iPdfTable = null;
    }
    
    public void printCell(ExamGridModel model, int day, int slot, int idx, int maxIdx, boolean head, boolean vertical, boolean in, boolean eod, boolean eol) {
        ExamPeriod period = getPeriod(day, slot);
        ExamGridCell cell = model.getAssignment(period,idx);
        PdfPCell c = createCell();
        c.setBorderWidthTop(head || (!in && !vertical) ? 1 : 0);
        c.setBorderWidthRight(eod || eol ? 1 : 0);
        if (cell==null) {
            String bgColor = model.getBackground(period);
            if (bgColor==null && !model.isAvailable(period)) bgColor=sBgColorNotAvailable;
            if (period==null) bgColor=sBgColorNotAvailable;
            if (bgColor!=null)
                c.setBackgroundColor(getColor(bgColor));
            c.setBorderWidthBottom(idx<maxIdx ? 0 : 1);
            addText(c, " ");
        } else {
            String bgColor = cell.getBackground();
            if (iForm.getBackground()==Background.None.ordinal() && !sBgColorNotAvailable.equals(bgColor) && !model.isAvailable(period)) {
                    bgColor = sBgColorNotAvailableButAssigned;
            }
            if (bgColor!=null)
                c.setBackgroundColor(getColor(bgColor));
            addText(c, cell.getName());
            if (iForm.getResource()!=Resource.Room.ordinal())
                addText(c, cell.getRoomName());
            else
                addText(c, cell.getShortCommentNoColors()==null?"":cell.getShortCommentNoColors());
        }
        iPdfTable.addCell(c);
    }
    
    public void printRowHeaderCell(String name, int idx, int maxIdx, boolean vertical, boolean head, boolean in) {
        PdfPCell c = createCell();
        c.setBorderWidthTop(idx==0 && (head || (!in && !vertical)) ? 1 : 0);
        c.setBorderWidthBottom(idx<maxIdx ? 0 : 1);
        c.setBorderWidthRight(0);
        if (idx==0) addText(c, name);
        iPdfTable.addCell(c);
    }
    
    public void printTable() throws Exception {
        boolean vertical = isVertical();
        TreeSet<Integer> days = days(), slots = slots(), weeks = weeks(), daysOfWeek = daysOfWeek();
        int rowNumber=0; 
        if (iForm.getDispMode()==DispMode.InRowVertical.ordinal()) {
            int globalMaxIdx = 0;
            for (Integer day:days) 
                for (Integer slot:slots) {
                    globalMaxIdx = Math.max(globalMaxIdx,getMaxIdx(day, slot));
                }
            int week = -1;
            for (Integer day:days) {
                boolean head = false;
                if (week!=getWeek(day)) {
                    week = getWeek(day);
                    head = true;
                    flushTable(); createTable(true);
                    printHeader(getWeekName(week));
                }
                for (Integer slot:slots) {
                    if (getPeriod(day, slot)==null) continue;
                    int maxIdx = getMaxIdx(day, slot);
                    for (int idx=0;idx<=maxIdx;idx++) {
                        printRowHeaderCell(getDayName(day)+"<br>"+getSlotName(slot), idx, maxIdx, vertical, head && slot.equals(slots.first()), globalMaxIdx==0);
                        for (ExamGridModel model : models()) {
                            printCell(model,
                                    day,
                                    slot,
                                    idx, maxIdx,
                                    head && slot.equals(slots.first()) && idx==0, vertical, globalMaxIdx==0 || idx>0,
                                    false, model.equals(models().lastElement()));
                        }
                    }
                    rowNumber++;
                }
            }
        } else {
            int tmx = 0;
            for (ExamGridModel m : models())
                tmx = Math.max(tmx,getMaxIdx(m, days.first(),days.last(),slots.first(),slots.last()));
            for (ExamGridModel model : models()) {
                if (iForm.getDispMode()==DispMode.InRowHorizontal.ordinal()) {
                    if (rowNumber==0) { createTable(false); printHeader(null); }
                    int maxIdx = getMaxIdx(model, days.first(),days.last(),slots.first(),slots.last());
                    for (int idx=0;idx<=maxIdx;idx++) {
                        printRowHeaderCell(model.getName()+(model.getSize()>0?" ("+model.getSize()+")":"")+"<br> ", idx, maxIdx, vertical, (rowNumber%10==0), tmx==0);
                        for (Integer day:days) {
                            for (Integer slot:slots) {
                                boolean eod = (slot.equals(slots.last()));
                                boolean eol = (eod && day.equals(days.last()));
                                printCell(model,
                                        day,
                                        slot,
                                        idx, maxIdx,
                                        rowNumber%10==0 && idx==0, vertical, tmx==0 || idx>0,
                                        eod, eol);
                            }
                        }
                    }
                } else if (iForm.getDispMode()==DispMode.PerDayVertical.ordinal()) {
                    flushTable(); createTable(true);
                    printHeader(getModelName(model));
                    int gmx = getMaxIdx(model, days.first(),days.last(),slots.first(),slots.last());
                    for (Integer slot:slots) {
                        int maxIdx = getMaxIdx(model, days.first(), days.last(), slot, slot);
                        for (int idx=0;idx<=maxIdx;idx++) {
                            printRowHeaderCell(getSlotName(slot)+"<br> ", idx, maxIdx, vertical, slot.equals(slots.first()), gmx==0);
                            for (Integer day:days) {
                                printCell(model,
                                        day,
                                        slot,
                                        idx, maxIdx,
                                        slot.equals(slots.first()) && idx==0, vertical, gmx==0 || idx>0,
                                        false, (day.equals(days.last())));
                            }
                        }
                    }
                } else if (iForm.getDispMode()==DispMode.PerDayHorizontal.ordinal()) {
                    flushTable(); createTable(true);
                    printHeader(getModelName(model));
                    int gmx = getMaxIdx(model, days.first(),days.last(),slots.first(),slots.last());
                    for (Integer day:days) {
                        int maxIdx = getMaxIdx(model, day, day,slots.first(),slots.last());
                        for (int idx=0;idx<=maxIdx;idx++) {
                            printRowHeaderCell(getDayName(day)+"<br> ", idx, maxIdx, vertical, day.equals(days.first()), gmx==0);
                            for (Integer slot:slots) {
                                printCell(model,
                                        day,
                                        slot,
                                        idx, maxIdx,
                                        day.equals(days.first()) && idx==0, vertical, gmx==0 || idx>0,
                                        false, (slot.equals(slots.last())));
                            }
                        }
                    }
                } else if (iForm.getDispMode()==DispMode.PerWeekHorizontal.ordinal()) {
                    flushTable(); createTable(true);
                    printHeader(getModelName(model));
                    int gmx = getMaxIdx(model, days.first(), days.last(), slots.first(),slots.last());
                    for (Integer dow:daysOfWeek()) {
                        int maxIdx = getMaxIdx(model, dow,slots.first(),slots.last());
                        for (int idx=0;idx<=maxIdx;idx++) {
                            printRowHeaderCell(getDayOfWeekName(dow)+"<br> ", idx, maxIdx, vertical, dow.equals(daysOfWeek.first()), gmx==0);
                            for (Integer week : weeks) {
                                for (Integer slot:slots) {
                                    printCell(model,
                                            getDay(week,dow),
                                            slot,
                                            idx, maxIdx,
                                            dow.equals(daysOfWeek.first()) && idx==0, vertical, gmx==0 || idx>0,
                                            (slot.equals(slots.last())), (slot.equals(slots.last()) && week.equals(weeks.last())));
                                }
                            }
                        }
                     }
                } else if (iForm.getDispMode()==DispMode.PerWeekVertical.ordinal()) {
                    flushTable(); createTable(true);
                    printHeader(getModelName(model));
                    int gmx = getMaxIdx(model, days.first(), days.last(), slots.first(),slots.last());
                    for (Integer week : weeks) {
                        for (Integer slot:slots) {
                            int maxIdx = getMaxIdx(model, week,slot);
                            for (int idx=0;idx<=maxIdx;idx++) {
                                printRowHeaderCell(getWeekName(week) +"<br>"+ getSlotName(slot), idx, maxIdx, vertical, slot.equals(slots.first()), gmx==0);
                                for (Integer dow : daysOfWeek) {
                                    printCell(model, 
                                            getDay(week,dow), 
                                            slot, 
                                            idx, 
                                            maxIdx, 
                                            slot.equals(slots.first()) && idx==0, vertical, gmx==0 || idx>0, 
                                            false, (dow.equals(daysOfWeek.last())));
                                }
                            }                            
                        }
                    }
                }
                rowNumber++;
            }
        }
        flushTable();
    }
    
    private void addLegendRow(String color, String text) {
        PdfPCell c = createCellNoBorder();
        c.setBorderWidth(1);
        c.setBackgroundColor(getColor(color));
        iPdfTable.addCell(c);
        c = createCellNoBorder();
        addText(c, "  "+text);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        iPdfTable.addCell(c);
    }
    
    public void printLegend() throws Exception {
        iPdfTable = new PdfPTable(2);
        iPdfTable.setWidths(new float[] {10f,200f});
        iPdfTable.getDefaultCell().setPadding(3);
        iPdfTable.getDefaultCell().setBorderWidth(1);
        iPdfTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        iPdfTable.setSplitRows(false);
        iPdfTable.setSpacingBefore(10);
        iPdfTable.setKeepTogether(true);
        
        if (iForm.getBackground()!=Background.None.ordinal()) {
            PdfPCell c = createCellNoBorder();
            c.setColspan(2);
            addText(c,MSG.propAssignedExaminations());
            c.setHorizontalAlignment(Element.ALIGN_LEFT);
            iPdfTable.addCell(c);
        }
        if (iForm.getBackground()==Background.PeriodPref.ordinal()) {
            addLegendRow(pref2color(PreferenceLevel.sRequired),MSG.legendRequiredPeriod());
            addLegendRow(pref2color(PreferenceLevel.sStronglyPreferred),MSG.legendStronglyPreferredPeriod());
            addLegendRow(pref2color(PreferenceLevel.sPreferred),MSG.legendPreferredPeriod());
            addLegendRow(pref2color(PreferenceLevel.sNeutral),MSG.legendNoPeriodPreference());
            addLegendRow(pref2color(PreferenceLevel.sDiscouraged),MSG.legendDiscouragedPeriod());
            addLegendRow(pref2color(PreferenceLevel.sStronglyDiscouraged),MSG.legendStronglyDiscouragedPeriod());
            addLegendRow(pref2color(PreferenceLevel.sProhibited),MSG.legendProhibitedPeriod());
        } else if (iForm.getBackground()==Background.RoomPref.ordinal()) {
            addLegendRow(pref2color(PreferenceLevel.sRequired),MSG.legendRequiredRoom());
            addLegendRow(pref2color(PreferenceLevel.sStronglyPreferred),MSG.legendStronglyPreferredRoom());
            addLegendRow(pref2color(PreferenceLevel.sPreferred),MSG.legendPreferredRoom());
            addLegendRow(pref2color(PreferenceLevel.sNeutral),MSG.legendNoRoomPreference());
            addLegendRow(pref2color(PreferenceLevel.sDiscouraged),MSG.legendDiscouragedRoom());
            addLegendRow(pref2color(PreferenceLevel.sStronglyDiscouraged),MSG.legendStronglyDiscouragedRoom());
            addLegendRow(pref2color(PreferenceLevel.sProhibited),MSG.legendProhibitedRoom());
        } else if (iForm.getBackground()==Background.InstructorConfs.ordinal()) {
            addLegendRow(pref2color(PreferenceLevel.sNeutral),MSG.legendNoInstructorConflict());
            addLegendRow(pref2color(PreferenceLevel.sDiscouraged),MSG.legendOneOrMoreInstructorBackToBackConflicts());
            addLegendRow(pref2color(PreferenceLevel.sStronglyDiscouraged),MSG.legendOneOrMoreInstructorThreeOrMoreExamsADayConflicts());
            addLegendRow(pref2color(PreferenceLevel.sProhibited),MSG.legendOneOrMoreInstructorDirectConflicts());
        } else if (iForm.getBackground()==Background.StudentConfs.ordinal()) {
            addLegendRow(pref2color(PreferenceLevel.sNeutral),MSG.legendNoStudentConflict());
            addLegendRow(pref2color(PreferenceLevel.sDiscouraged),MSG.legendOneOrMoreStudentBackToBackConflicts());
            addLegendRow(pref2color(PreferenceLevel.sStronglyDiscouraged),MSG.legendOneOrMoreStudentThreeOrMoreExamsADayStudentConflicts());
            addLegendRow(pref2color(PreferenceLevel.sProhibited),MSG.legendOneOrMoreStudentDirectConflicts());
        } else if (iForm.getBackground()==Background.DirectInstructorConfs.ordinal()) {
            for (int nrConflicts=0;nrConflicts<=6;nrConflicts++) {
                String color = lessConflicts2color(nrConflicts);
                addLegendRow(color,MSG.legendInstructorConflicts(nrConflicts+(nrConflicts==6?" "+MSG.legendOrMore():"")));
            }
        } else if (iForm.getBackground()==Background.MoreThanTwoADayInstructorConfs.ordinal()) {
            for (int nrConflicts=0;nrConflicts<=15;nrConflicts++) {
                String color = conflicts2color(nrConflicts);
                addLegendRow(color,MSG.legendInstructorMoreThanTwoExamsADayConflicts(nrConflicts+(nrConflicts==15?" "+MSG.legendOrMore():"")));
            }
        } else if (iForm.getBackground()==Background.BackToBackInstructorConfs.ordinal()) {
            for (int nrConflicts=0;nrConflicts<=15;nrConflicts++) {
                String color = conflicts2color(nrConflicts);
                addLegendRow(color,MSG.legendInstructorBackToBackConflicts(nrConflicts+(nrConflicts==15?" "+MSG.legendOrMore():"")));
            }
        } else if (iForm.getBackground()==Background.DirectStudentConfs.ordinal()) {
            for (int nrConflicts=0;nrConflicts<=6;nrConflicts++) {
                String color = lessConflicts2color(nrConflicts);
                addLegendRow(color,MSG.legendStudentDirectConflicts(nrConflicts+(nrConflicts==6?" "+MSG.legendOrMore():"")));
            }
        } else if (iForm.getBackground()==Background.MoreThanTwoADayStudentConfs.ordinal()) {
            for (int nrConflicts=0;nrConflicts<=15;nrConflicts++) {
                String color = conflicts2color(nrConflicts);
                addLegendRow(color,MSG.legendStudentMoreThanTwoExamsADayConflicts(nrConflicts+(nrConflicts==15?" "+MSG.legendOrMore():"")));
            }
        } else if (iForm.getBackground()==Background.BackToBackStudentConfs.ordinal()) {
            for (int nrConflicts=0;nrConflicts<=15;nrConflicts++) {
                String color = conflicts2color(nrConflicts);
                addLegendRow(color,MSG.legendStudentBackToBackConflicts(nrConflicts+(nrConflicts==15?" "+MSG.legendOrMore():"")));
            }
        } else if (iForm.getBackground()==Background.DistPref.ordinal()) {
            addLegendRow(pref2color(PreferenceLevel.sNeutral),MSG.legendNoViloatedDistributionConstraint());
            addLegendRow(pref2color(PreferenceLevel.sDiscouraged),MSG.legendDiscouragedOrPreferredDistributionConstraintViolated());
            addLegendRow(pref2color(PreferenceLevel.sStronglyDiscouraged),MSG.legendStronglyDiscouragedOrPreferredDistributionConstraintViolated());
            addLegendRow(pref2color(PreferenceLevel.sProhibited),MSG.legendRequiredOrProhibitedDistributionConstraintViolated());
        }
        PdfPCell c = createCellNoBorder();
        c.setColspan(2);
        addText(c,MSG.propFreeTimes());
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        iPdfTable.addCell(c);
        addLegendRow(sBgColorNotAvailable,MSG.legendPeriodNotAvailable());
        if (iForm.getBgPreferences() && iForm.getBackground()==Background.PeriodPref.ordinal()) {
            addLegendRow(pref2color(PreferenceLevel.sStronglyPreferred),MSG.legendStronglyPreferredPeriod());
            addLegendRow(pref2color(PreferenceLevel.sPreferred),MSG.legendNoPeriodPreference());
        }
        addLegendRow(pref2color(PreferenceLevel.sNeutral),MSG.legendNoPeriodPreference());
        if (iForm.getBgPreferences() && iForm.getBackground()==Background.PeriodPref.ordinal()) {
            addLegendRow(pref2color(PreferenceLevel.sDiscouraged),MSG.legendDiscouragedPeriod());
            addLegendRow(pref2color(PreferenceLevel.sStronglyDiscouraged),MSG.legendStronglyPreferredPeriod());
            addLegendRow(pref2color(PreferenceLevel.sProhibited),MSG.legendProhibitedPeriod());
        }
        
        iDocument.add(iPdfTable);
    }
    
}