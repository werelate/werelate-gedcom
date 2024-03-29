package org.werelate.gedcom;

import org.werelate.util.PlaceUtils;
import org.werelate.util.Utils;
import org.werelate.util.EventDate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.io.PrintWriter;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Oct 13, 2006
 * Time: 11:11:21 AM
 * Represents a WeRelate person and a GEDCOM INDI
 */
public class Person extends EventContainer implements Comparable {
   private static final Logger logger = LogManager.getLogger("org.werelate.gedcom.Person");
   private static final int CURR_YEAR = (new GregorianCalendar()).get(Calendar.YEAR);

   String primaryChildOf = null;
   // Sometimes used to indicate a childOfFamily as
   // being "adoptive" or "birthDates" parents.
   private String pedi = null;
   // Family ID indicating which childOfFamily fits
   // the "pedi" descriptor word
   private String pediID = null;
   // Originally from the "STAT" field
   private String status = null;

   /**
    * This is used primarily to print the status of a child of a family
    * @return the status of the person, as originally found in a field such as "STAT"
    *
    */
   public String getStatus() {
      return status;
   }
   /**
    * Set the status of the person to be printed as the child's status in a family
    * @param status
    */
   public void setStatus(String status) {
      this.status = status;
   }
   // Todos are text fields similar to notes
   private Collection <String> todos = new ArrayList <String> ();

   /**
    *
    * @param newTodo
    */
   public void addTodo(String newTodo)
   {
      todos.add(newTodo);
   }
   public Collection<String> getTodos() {
      return todos;
   }
   private boolean isAdopted = false;

   /**
    *
    * @return whether this person is adopted, used
    * for indicating whether children are adopted
    */
   public boolean isAdopted() {
      return isAdopted;
   }

   /**
    * Set whether person is adopted
    * @param adopted
    */
   public void setAdopted(boolean adopted) {
      isAdopted = adopted;
   }

   private Set<String> childOfFamilies = new HashSet<String>();
   /**
    *
    * @param childOfFamily is a gedcom ID of family in which this person is a child
    * @param isPrimary set true if this childOfFamily should be listed first
    * @param pedi is string indicating the relationship with the parents in childOfFamily
    */
   public void addChildOfFamily(String childOfFamily, boolean isPrimary, String pedi)
   {
      if (childOfFamily != null) { // not sure if this is necessary
         if (isPrimary || childOfFamilies.isEmpty())
         {
            primaryChildOf = childOfFamily;
         }
         if (!Utils.isEmpty(pedi))
         {
            this.pedi = pedi;
            pediID = childOfFamily;
         }
         childOfFamilies.add(childOfFamily);
      }
   }

   public String getPrimaryChildOf() {
      return primaryChildOf;
   }

   /**
    *
    * @return a set of all ID numbers of families where this person is a child
    */
   public Set<String> getChildOfFamilies() {
      return childOfFamilies;
   }

   private Set<String> spouseOfFamilies = new HashSet<String>();

   /**
    *
    * @param spouseOfFamily gedcom ID number of family where this person is a spouse
    */
   public void addSpouseOfFamily(String spouseOfFamily)
   {
      if (spouseOfFamily != null) { // gedcom 7134 had this problem?
         spouseOfFamilies.add(spouseOfFamily);
      }
   }

   /**
    *
    * @return set of all gedcom ID numbers of families where this person is a spouse
    */
   public Set<String> getSpouseOfFamilies() {
      return spouseOfFamilies;
   }

   public Collection<String> getFamilies() {
      List<String> families = new ArrayList<String>(spouseOfFamilies.size()+childOfFamilies.size());
      families.addAll(spouseOfFamilies);
      families.addAll(childOfFamilies);
      return families;

   }

   /**
    * Implementation of standard Comparable.compareTo
    * @param o other Person object to be compared to. Note:
    *    A class cast exception will be thrown if this is not the
    *    case
    * @return int indicating the ordering of the two objects
    */
   public int compareTo(Object o) {
      Person other = (Person) o;
      Integer thisBirthDate = new EventDate(getBirthDate()).getDateSortKey();                 // method replaced Oct 2021 by Janet Bjorndahl
      Integer otherBirthDate = new EventDate(other.getBirthDate()).getDateSortKey();          // method replaced Oct 2021 by Janet Bjorndahl
      int rval = thisBirthDate.compareTo(otherBirthDate);
      if (rval != 0)
      {
         return rval;
      }
      
      // If the birthdates are the same, then
      // we'll use the name prepended to the id
      // as the sort key.
      String sort1 = getID();
      /*
      * TODO: Phase III: sort the children according to their names.
      if (!Utils.isEmpty(getReservedTitle()))
      {
         sort1 = getReservedTitle();
      } else
      {
         sort1 = getName().toString() + getID();
      }*/
      String sort2 = other.getID();
      /*
      if (!Utils.isEmpty(other.getReservedTitle()))
      {
         sort2 = other.getReservedTitle();
      } else
      {
         sort2 = other.getName().toString() + other.getID();
      } */
      return sort1.compareTo(sort2);
   }

   public static enum Gender { male, female, unknown}
   private Gender gender = Gender.unknown;
   public Gender getGender () {
      return gender;
   }
   public void setGender(Gender gender) {
      this.gender = gender;
   }

   /**
    * Attempts to parse out the single leter indication of Gender
    * @param newGender should be "M" or "F"
    */
   public void setGender(String newGender) {
      if (newGender.equals("M")) {
         setGender(Gender.male);
      } else if (newGender.equals("F")) {
         setGender(Gender.female);
      } else {
         setGender(Gender.unknown);
      }
   }

   /**
    *
    * @return "M" if person is male, "F" if person is female, "" otherwise
    */
   public String getGenderString() {
      switch (getGender())
      {
         case male:
            return "M";
         case female:
            return "F";
         default:
            return "";
      }
   }

   public static enum LivingStatus {
      LIVING, DEAD, UNKNOWN
   }
   private LivingStatus isLiving = LivingStatus.UNKNOWN;
   public LivingStatus getLiving() {
      return isLiving;
   }
   public boolean isLiving() {
      return getLiving() == LivingStatus.LIVING;
   }

   /**
    * Sets the living status of the person, and
    * resets the wikiTitle = null because it may
    * have changed
    * @param living
    */
   public void setLiving(LivingStatus living) {
      isLiving = living;
      resetWikiTitle();
   }

   public void resetWikiTitle () {
      this.wikiTitle= null;
   }

   private static final int DEAD_IF_OLDER_THAN = 110;
   private static final int DEAD_IF_MARRIED = 90;
   private static final int LIVING_IF_PARENTS_YOUNGER_THAN = 130;
   private static final int LIVING_IF_PARENTS_MARRIED = 110;
   private static final int LIVING_IF_CHILD_YOUNGER_THAN = 90;
   private static final int LIVING_IF_SIBLING_YOUNGER_THAN = 110;

   private static final Set<String> LIVING_EVENT_WORDS =
           new HashSet<String>(Arrays.asList("living","private","alive","details withheld"));

   private boolean isDeadText(String s) {
      if (s != null) s = s.toLowerCase();
      return !Utils.isEmpty(s) && !LIVING_EVENT_WORDS.contains(s) && !"n".equals(s.toLowerCase());
   }

   // Determines if this person is definitely
   // dead based on whether this person has a death
   // or burial event, a birth event which says the
   // person was born more than DEAD_IF_OLDER_THAN
   // years ago, or a marriage even more than
   // DEAD_IF_MARRIED years ago.
   private boolean isDefinitelyDead(Gedcom gedcom)
         throws Gedcom.PostProcessException
   {
      boolean rval = false;
      for (Event event : getEvents())
      {
         if ((event.getType() == Event.Type.death ||
               event.getType() == Event.Type.burial) &&
               (isDeadText(event.getAttribute("DATE")) ||
                isDeadText(event.getAttribute("PLAC")) ||
                isDeadText(event.getContent()) ||
                isDeadText(event.getDescription())))
         {
            rval = true;
            break;
         } else if (event.getType() == Event.Type.birth
               || event.getType() == Event.Type.christening
               || event.getType() == Event.Type.Baptism)
         {
            String date = event.getAttribute("DATE");
            if (!Utils.isEmpty(date))
            {
               rval = isDateThatOld(date, DEAD_IF_OLDER_THAN);
            }
            if (rval) break;
         }
      }

      // Now we need to check the marriage dates
      // of all of the families in which I am a
      // spouse
      if (!rval)
      {
         for (String famID : getSpouseOfFamilies())
         {
            Family fam = gedcom.getFamilies().get(famID);
            if (fam != null)
            {
               for (Event event : fam.getEvents())
               {
                  // lots of different kinds of marriage events, so just look at all of them
                  String date = event.getAttribute("DATE");
                  if (!Utils.isEmpty(date))
                  {
                     if (rval = isDateThatOld(date, DEAD_IF_MARRIED)) break;
                  }
               }
            } else
            {
               logger.info("While processing person " + getID() + "," +
                     " Invalid Spouse of Family ID: " + famID);
            }
         }
      }
      return rval;
   }

   private boolean hasLivingEvents(int cutoffBirth) {
      for (Event event : getEvents())
      {
         String date = event.getAttribute("DATE");
         if (!Utils.isEmpty(date))
         {
            date = date.trim().toLowerCase();
            if (LIVING_EVENT_WORDS.contains(date) ||
                ((event.getType() == Event.Type.alt_birth ||
                  event.getType() == Event.Type.birth ||
                  event.getType() == Event.Type.Baptism ||
                  event.getType() == Event.Type.christening ||
                  event.getType() == Event.Type.alt_christening) &&
                 isDateNewerThan(date, cutoffBirth)))
            {
               return true;
            }
         }
         if (event.getType() == Event.Type.death) {
            if ((!Utils.isEmpty(event.getAttribute("DATE")) && "n".equals(event.getAttribute("DATE").toLowerCase())) ||
                (!Utils.isEmpty(event.getAttribute("PLAC")) && "n".equals(event.getAttribute("PLAC").toLowerCase())) ||
                (!Utils.isEmpty(event.getContent()) && "n".equals(event.getContent().toLowerCase())) ||
                (!Utils.isEmpty(event.getDescription()) && "n".equals(event.getDescription().toLowerCase()))) {
               return true;
            }
         }
      }
      return false;
   }

   private boolean hasEventsBeforeCutoff(int cutOffDay) {
      for (Event event : getEvents())
      {
         String date = event.getAttribute("DATE");
         if (!Utils.isEmpty(date))
         {
            date = date.trim().toLowerCase();
            int maxDay = new EventDate(date).getMaxDay();                      // method replaced Oct 2021 by Janet Bjorndahl
            if (maxDay != 0 && maxDay < cutOffDay) {
               return true;
            }
         }
      }
      return false;
   }

   private boolean hasBirthEventAfterCutoff(int cutOffDay) {
      for (Event event : getEvents())
      {
         String date = event.getAttribute("DATE");
         if ((event.getType() == Event.Type.alt_birth ||
              event.getType() == Event.Type.birth ||
              event.getType() == Event.Type.Baptism ||
              event.getType() == Event.Type.christening ||
              event.getType() == Event.Type.alt_christening) &&
             !Utils.isEmpty(date)) {
            date = date.trim().toLowerCase();
            int maxDay = new EventDate(date).getMaxDay();                      // method replaced Oct 2021 by Janet Bjorndahl
            if (maxDay > cutOffDay) {
               return true;
            }
         }
      }
      return false;
   }

   public boolean hasOldBirth(int numYears) {
      for (Event event : getEvents())
      {
         if (event.getType() == Event.Type.birth
               || event.getType() == Event.Type.christening
               || event.getType() == Event.Type.Baptism)
         {
            String date = event.getAttribute("DATE");
            if (!Utils.isEmpty(date) && isDateThatOld(date, numYears)) {
               return true;
            }
         }
      }
      return false;
   }

   public boolean hasOldRelatives(Gedcom gedcom) {
      for (String familyID : getChildOfFamilies()) {
         Family family = gedcom.getFamilies().get(familyID);
         if (family != null && family.hasOldFamilyMember(gedcom, DEAD_IF_OLDER_THAN+70, DEAD_IF_OLDER_THAN+50)) {
            return true;
         }
      }
      for (String familyID : getSpouseOfFamilies()) {
         Family family = gedcom.getFamilies().get(familyID);
         if (family != null && family.hasOldFamilyMember(gedcom, DEAD_IF_OLDER_THAN+50, DEAD_IF_OLDER_THAN-20)) {
            return true;
         }
      }
      return false;
   }

   // If someone named "Living" isn't marked living, change their name to Unknown
   public static void setUnknownName(Gedcom gedcom) {
      for (Person person : gedcom.getPeople().values())
      {
         if (!person.isLiving()) {
            Name name = person.getName();
            if (name != null) {
               String given = name.getGiven();
               String surname = name.getSurname();
               if (!PlaceUtils.isEmpty(given) && given.equalsIgnoreCase("living")) {
                  name.clearGiven();
                  name.setGiven("Unknown");
               }
               if (PlaceUtils.isEmpty(given) && !PlaceUtils.isEmpty(surname) && surname.equalsIgnoreCase("living")) {
                  name.clearSurname();
                  name.clearGiven();
                  name.setGiven("Unknown");
               }
            }
         }
      }
   }

   // This method assumes that the method
   // "isDefinitelyDead" has already been
   // run and returned false
   private boolean isDefinitelyLiving(Gedcom gedcom) throws Gedcom.PostProcessException {
      boolean rval = false;

      // First let's check to see if the
      // gedcom generator has designated this person as living
      if (getEvents().size() == 0)
      {
         Name name = getName();
         if (name != null) {
            // person is living if their given name is living or their given name is empty and their surname is living, and they don't have old relatives
            if (((!PlaceUtils.isEmpty(name.getGiven()) && name.getGiven().equalsIgnoreCase("living")) ||
                 (PlaceUtils.isEmpty(name.getGiven()) && !PlaceUtils.isEmpty(name.getSurname()) && name.getSurname().equalsIgnoreCase("living"))) &&
                !hasOldRelatives(gedcom)) {
               return true;
            }
         }
      }
      else {
         // If there are events, we want to see if any of the
         // events mark this person as "Private".
         // If it does, we will assume the person is living.
         if (hasLivingEvents(DEAD_IF_OLDER_THAN)) {
            return true;
         }
      }

      // let's go through all of the marriage dates
      // and see if they are private or if they occur since DEAD_IF_MARRIED.
      // If so, we assume the person is living.
      for (String familyId : getSpouseOfFamilies())
      {
         Family family;
         if ((family = gedcom.getFamilies().get(familyId)) != null)
         {
            for(Event event : family.getEvents())
            {
               String date = event.getAttribute("DATE");
               if (!Utils.isEmpty(date))
               {
                  date = date.trim().toLowerCase();
                  if ((event.getType() == Event.Type.marriage || event.getType() == Event.Type.alt_marriage) &&
                      (date.equals("private") || date.equals("living") || isDateNewerThan(date, DEAD_IF_MARRIED)))
                  {
                     return true;
                  }
               }
            }

            // check for recently-born spouse
            for (String spouseId : family.getSpouses()) {
               Person spouse;
               if ((spouse = gedcom.getPeople().get(spouseId)) != null) {
                  if (spouse != this && // already checked this spouse
                      spouse.hasLivingEvents(DEAD_IF_OLDER_THAN)) {
                     return true;
                  }
               }
               else {
                  logger.info("While processing family " + familyId + "," +
                           " Invalid Spouse ID: " + spouseId);
               }
            }

            // check for recently-born child
            for (Family.Child c : family.getChildren()) {
               Person child;
               if ((child = gedcom.getPeople().get(c.getId())) != null) {
                  if (child.hasLivingEvents(LIVING_IF_CHILD_YOUNGER_THAN)) {
                     return true;
                  }
               }
               else {
                  logger.info("While processing family " + familyId + "," +
                           " Invalid Child ID: " + c.getId());
               }
            }
         }
         else
         {
            logger.info("While processing person " + getID() + "," +
                     " Invalid Spouse of Family ID: " + familyId);
         }
      }

      // check parent families for recently-married parents, living parents
      for (String familyId : getChildOfFamilies())
      {
         Family family;
         if ((family = gedcom.getFamilies().get(familyId)) != null)
         {
            for(Event event : family.getEvents())
            {
               String date = event.getAttribute("DATE");
               if (!Utils.isEmpty(date))
               {
                  date = date.trim().toLowerCase();
                  if ((event.getType() == Event.Type.marriage || event.getType() == Event.Type.alt_marriage) &&
                      (date.equals("private") || date.equals("living") || isDateNewerThan(date, LIVING_IF_PARENTS_MARRIED)))
                  {
                     return true;
                  }
               }
            }

            // check for recently-born parent
            for (String spouseId : family.getSpouses()) {
               Person spouse;
               if ((spouse = gedcom.getPeople().get(spouseId)) != null) {
                  if (spouse.hasLivingEvents(LIVING_IF_PARENTS_YOUNGER_THAN)) {
                     return true;
                  }
               }
               else {
                  logger.info("While processing family " + familyId + "," +
                           " Invalid Spouse ID: " + spouseId);
               }
            }

            // check for recently-born siblings
            for (Family.Child c : family.getChildren()) {
               Person child;
               if ((child = gedcom.getPeople().get(c.getId())) != null) {
                  if (child.hasLivingEvents(LIVING_IF_SIBLING_YOUNGER_THAN)) {
                     return true;
                  }
               }
               else {
                  logger.info("While processing family " + familyId + "," +
                           " Invalid Child ID: " + c.getId());
               }
            }
         }
         else
         {
            logger.info("While processing person " + getID() + "," +
                     " Invalid Child of Family ID: " + familyId);
         }
      }

      return false;
   }

   /**
    * @param date to compare to
    * @param numYearsAgo
    * @return true if the date is at least more than numYearsAgo, false otherwise
    */
   public static boolean isDateThatOld(String date, int numYearsAgo) {
      int maxDay = new EventDate(date).getMaxDay();                        // method replaced Oct 2021 by Janet Bjorndahl
      return (maxDay != 0 && CURR_YEAR - (maxDay / 365) >= numYearsAgo);
   }

   // Returns true if date is sooner than numYearsAgo
   private boolean isDateNewerThan(String date, int numYearsAgo) {
      int minDay = new EventDate(date).getMinDay();                        // method replaced Oct 2021 by Janet Bjorndahl
      return (minDay != 0 && CURR_YEAR - (minDay / 365) < numYearsAgo);
//      logger.warn("isDateNewerThan date="+date+" minDay/365="+(minDay/365)+" curr_year="+CURR_YEAR+" numYearsAgo="+numYearsAgo+" return="+r);
   }

   private void setLivingFirstPass(Gedcom gedcom) throws Gedcom.PostProcessException {
      if (isDefinitelyDead(gedcom))
      {
         setLiving(LivingStatus.DEAD);
      } else if (isDefinitelyLiving(gedcom))
      {
         setLiving(LivingStatus.LIVING);
      }
   }

   // After we have done our best to determine whether someone
   // is living or dead using the persons own events, such as
   // birth, death, and marriage events, now we will try to
   // use the person's relatives to guess as to whether he is
   // living or dead.
   //
   // Mark a living person's unknown-status spouse, children, and siblings as living
   private void setLivingSecondPass(Gedcom gedcom) throws Uploader.PrintException, Gedcom.PostProcessException
   {
      if (getLiving() == LivingStatus.LIVING) {
         markFamilyMembersLiving(getChildOfFamilies(), gedcom, false);
         markFamilyMembersLiving(getSpouseOfFamilies(), gedcom, true);
      }
   }

   private void markFamilyMembersLiving(Set<String> families, Gedcom gedcom, boolean includeSpouses) {
      for (String famID : families) {
         Family fam = gedcom.getFamilies().get(famID);
         if (fam != null) {
            if (includeSpouses) {
               for (String personID : fam.getSpouses()) {
                  Person p = gedcom.getPeople().get(personID);
                  if (p != null && p.getLiving() == LivingStatus.UNKNOWN) {
                     p.setLiving(LivingStatus.LIVING);
                  }
               }
            }
            for (Family.Child child : fam.getChildren()) {
               Person p = gedcom.getPeople().get(child.getId());
               if (p != null && p.getLiving() == LivingStatus.UNKNOWN) {
                  p.setLiving(LivingStatus.LIVING);
               }
            }
         }
         else
         {
            logger.info("While processing person " + getID() +
                  ", family ID \"" + famID + "\" is invalid!");
         }
      }
   }

   /**
    * Sets the living status of all individuals
    * in the gedcom passed into this method
    */
   public static void setLiving (Gedcom gedcom) throws Gedcom.PostProcessException
   {
      // try to mark people living or dead based upon dates of themselves or their near relatives
      for (Person person : gedcom.getPeople().values())
      {
         try
         {
            person.setLivingFirstPass(gedcom);
         } catch (Gedcom.PostProcessException e)
         {
            gedcom.warn("Caught post process exception while attempting to set person \"" +
                        person.getID() + "\"'s isLiving status in first pass: " + e);
         }
      }

      boolean foundLiving = true;
      Set<String> livingIds = new HashSet<String>();
      while (foundLiving) {
         foundLiving = false;
         for (Person person : gedcom.getPeople().values())
         {
            try
            {
               if (person.getLiving() == LivingStatus.LIVING && livingIds.add(person.getID())) {
                  foundLiving = true;
                  person.setLivingSecondPass(gedcom);
               }
            } catch (Uploader.PrintException e)
            {
               gedcom.warn("Caught post process exception while attempting to set person \"" +
                           person.getID() + "\"'s isLiving status in second pass: " + e);
            }
         }
      }

      // mark everyone else dead
      for (Person person : gedcom.getPeople().values()) {
         if (person.getLiving() == LivingStatus.UNKNOWN) {
            person.setLiving(LivingStatus.DEAD);
         }
      }
   }

   private boolean bornBeforeCutoff = false;
   public boolean isBornBeforeCutoff() {
      return bornBeforeCutoff;
   }
   // set this person and all ancestors born before cutoff
   public void setBornBeforeCutoff(Gedcom gedcom) {
      bornBeforeCutoff = true;
      for (String famID : getChildOfFamilies())
      {
         Family fam = gedcom.getFamilies().get(famID);
         if (fam != null)
         {
            for (String personID : fam.getSpouses()) {
               Person p = gedcom.getPeople().get(personID);
               if (p != null && !p.isBornBeforeCutoff() && !p.hasBirthEventAfterCutoff(gedcom.getCutoffDay())) {
                  p.setBornBeforeCutoff(gedcom);
               }
            }
         }
      }
      // assume children and spouse without birthdates are also born before cutoff
      for (String famID : getSpouseOfFamilies()) {
         Family fam = gedcom.getFamilies().get(famID);
         if (fam != null) {
            Collection<String> spouseChildren = new ArrayList<String>();
            spouseChildren.addAll(Family.getChildIDs(fam.getChildren()));
            spouseChildren.addAll(fam.getSpouses());
            for (String personID : spouseChildren) {
               Person p = gedcom.getPeople().get(personID);
               if (p != null && !p.isBornBeforeCutoff() && !p.hasBirthEventAfterCutoff(gedcom.getCutoffDay())) {
                  p.setBornBeforeCutoff(gedcom);
               }
            }
         }
      }
   }
   public static void setAllBornBeforeCutoff(Gedcom gedcom) {
      for (Person person: gedcom.getPeople().values()) {
         if (person.hasEventsBeforeCutoff(gedcom.getCutoffDay())) {
            person.setBornBeforeCutoff(gedcom);
         }
      }
   }

   private List <AlternateName> altNames = new ArrayList <AlternateName> ();
   private String marriedName = null;

   public String getMarriedName() {
      return marriedName;
   }

   /**
    * Adds an alternate name of type "Married Name"
    * @param marriedName
    * @param gedcom
    */
   public void addMarriedName(String marriedName, Gedcom gedcom) {
      AlternateName an = new AlternateName("Married Name");
      if (marriedName.indexOf('/') > 0 || marriedName.indexOf(',') > 0) {
         an.setName(marriedName, gedcom);
      }
      else {
         an.setSurname(marriedName);
      }
      addAltName(an);
   }

   /**
    * Adds a religious name of type "Religious Name"
    * @param religiousName
    * @param gedcom
    */
   public void addReligiousName(String religiousName, Gedcom gedcom) {
      AlternateName an = new AlternateName("Religious Name");
      an.setName(religiousName, gedcom);
      addAltName(an);
   }

   /**
    *
    * @return all alternate names attached to the person
    */
   public List <AlternateName> getAltNames() {
      return altNames;
   }

   /**
    * Adds an alternate name of type "Alt Name"
    * @param nickname
    */
   public void addAltName(AlternateName nickname) {
      if (nickname != null &&
            !Utils.isEmpty(nickname.toString()))
      {
         altNames.add(nickname);
      }
   }

   private Name name = null;

   /**
    *
    * @return the primary name of the person
    */
   public Name getName() {
      return name;
   }

   /**
    * Adds a name to the person. If the person
    * already has a primary name, then the new name
    * is added as an alternate name
    * @param name to add
    */
   public void addName(Name name) {
      if (getName() == null || Utils.isEmpty(getName().toString()))
      {
         this.name = name;
//         if (!Utils.isEmpty(getTitle()))
//         {
//            this.name.setPrefix(getTitle());
//         }
      } else
      {
         addAltName(new AlternateName(name));
      }
   }
   private String wikiTitle = null;

   /**
    * Returns the prepared wiki title of the person
    * based on the person's name and living status.
    * This method also caches the prepared wikiTitle
    *
    * If the person does not have a valid name, then
    * "Unknown" is returned
    * @param gedcom
    * @return
    * @throws Gedcom.PostProcessException
    */
   public String getWikiTitle(Gedcom gedcom) throws Gedcom.PostProcessException {
      if (Utils.isEmpty(wikiTitle))
      {
         if (getName() != null)
         {
            wikiTitle = getWikiTitle(getName());
         } else
         {
            logger.info(gedcom.logStr("Invalid name for person " + getID()));
            wikiTitle = "Unknown";
         }
      }
      return wikiTitle;
   }

   private static boolean isUnknownName(String name) {
      if (name == null || name.length() == 0) return true;
      name = name.toLowerCase();
      return (name.equals("unknown")
           || name.equals("unk")
           || name.equals("fnu")
           || name.equals("lnu")
           || name.equals("living")
           || name.equals("father")
           || name.equals("mother")
           || name.equals("?")
           || name.replace(".","").replace(" ","").equals("nn")
           || name.equals("private"));
   }

   // keep in sync with StructuredData.constructName
   public static String getWikiTitle(Name name)
   {
      String returnValue;
      String surname = Utils.capitalizeName(name.getTitleSurname(), false);
      // don't do this anymore
      //surname = cutOffSurname(surname);
      String given = Utils.capitalizeName(name.getFirstGiven(), true);
      boolean isSurnameUnknown = isUnknownName(surname);
      boolean isGivenUnknown = isUnknownName(given);
      if (isSurnameUnknown && isGivenUnknown) {
         returnValue = "Unknown";
      }
      else if (isGivenUnknown) {
         returnValue = "Unknown " + surname;
      }
      else if (isSurnameUnknown) {
         returnValue = given + " Unknown";
      }
      else {
         returnValue = given+" "+surname;
      }
      return Utils.prepareWikiTitle(returnValue);
   }

   /**
    *
    * @param surname
    * @return surname that has been shortened to only have at most three words
    */
   public static String cutOffSurname(String surname) {
      if (!Utils.isEmpty(surname))
      {
         surname = surname.trim();
         String [] surnameWords = surname.split("\\b\\W+\\b");
         if (surnameWords.length > 3)
         {
            int i = surname.indexOf(surnameWords[2]);
            surname = surname.substring(0, i + surnameWords[2].length());
         }
      }
      return surname;
   }
   // Simple utility function used by getWikiTitle which concatenates
   // the two parts of the name together
   private static String concatSurnameToGiven(String given, String surname) {
      String rval;
      if (surname == null)
      {
         rval = given + " Unknown";
      }
      else
      {
         rval = given + ' ' + surname;
      }
      return rval;
   }

   // Ancestral file number
   private String afn = null;

   public String getAfn() {
      return afn;
   }

   public void setAfn(String afn) {
      this.afn = Utils.setVal(this.afn, afn);
   }

   // GEDCOM TAGs that are okay inside of a person (INDI) tag
   private static final Set <String> ATTRIBUTES = new HashSet<String>(Arrays.asList(
           "NAME", "NAMR", "SEX", "NOTE", "HIST", "MIL",
           "_CEN", "INFO", "FAMC", "FAMILY_CHILD",
           "FAMS", "FAMILY_SPOUSE", "AFN", "TITL", "TITLE",
           "SOUR", "SOURCE", "DSCR", "REFN", "OBJE",
           "ALIA", "ALIAS", "NICK", "_TODO", "UID", "_UID"

   ));
   public static boolean isAttribute(String localName) {
      return ATTRIBUTES.contains(localName);
   }

   // Tags that we want to ignore yet still copy to
   // the text of the Person page
   private static Set <String> IGNORE_TAGS = new HashSet<String>(Arrays.asList(
           "DEST", "URL", "_URL", "HEAL",
           "FAM", "FAMILY", "EYES", "_AKAN", "HAIR", "COLO",
           "LANG", "HEIG", "_NMAR", "TEMP", "PLAC", "NOTEImmigrated",
           "LVG", "HM", "IDNO", "NMR", "_STAT", "NCHI", "_IFLAGS",
           "LOC", "WINF", "FISC", "GARD", "TEAC", "EARL", "MILF",
           "MAID"
   ));

   /**
    *
    * @param localName
    * @return true if localName represents a tag that should be ignored, false otherwise
    */
   public static boolean shouldIgnore (String localName)
   {
      return IGNORE_TAGS.contains(localName);
   }

   protected int getNamespace () {
      return Utils.PERSON_NAMESPACE;
   }

   private boolean shouldAlwaysPrint = false;
   public void setShouldAlwaysPrint (Gedcom gedcom, int numGenerations)
   {
      if (numGenerations <= 0) return;

      if (isLiving()) {
         shouldAlwaysPrint = true;
      }

      for (String famID : this.getChildOfFamilies())
      {
         Family fam = gedcom.getFamilies().get(famID);
         if (fam != null)
         {
            fam.setShouldAlwaysPrint();
            for (String husbID : fam.getHusbands())
            {
               Person husband = gedcom.getPeople().get(husbID);
               husband.setShouldAlwaysPrint(gedcom, numGenerations - 1);
            }
            for (String wifeID : fam.getWives())
            {
               Person wife = gedcom.getPeople().get(wifeID);
               wife.setShouldAlwaysPrint(gedcom, numGenerations - 1);
            }
         } else
         {
            gedcom.infoLine("Family ID is invalid: " + famID);
         }
      }
   }

   // Looks at the families this person belongs to and
   // determins whether any of them contain people who
   // are dead
   private boolean shouldPrintBasedOnFamilies(Gedcom gedcom, Set<String> families)
         throws Uploader.PrintException
   {
      for (String famID : families)
      {
         Family fam = gedcom.getFamilies().get(famID);
         if (fam != null)
         {
            if (fam.shouldPrint(gedcom))
            {
               return true;
            }
         } else
         {
            logger.info ("Invalid family ID: " + famID);
         }
      }
      return false;
   }

   public boolean isBeforeCutoff(Gedcom gedcom) {
      if (isBornBeforeCutoff()) {
         for (String famID : getFamilies()) {
            Family fam = gedcom.getFamilies().get(famID);
            if (fam != null && !fam.isBeforeCutoff(gedcom)) {
               return false;
            }
         }
         return true;
      }
      return false;
   }

   // empty if no events and no families
   public boolean isEmpty() {
//      boolean hasName = (getName() == null ||
//               (Utils.isUnknownName(getName().getTitleSurname()) && Utils.isUnknownName(getName().getFirstGiven())));
      boolean hasFamilies = (getSpouseOfFamilies().size() > 0 || getChildOfFamilies().size() > 0);
      boolean hasData = (getAltNames().size() > 0 || getEvents().size() > 0 || getNotes().size() > 0 || getCitations().size() > 0);
      return !hasData && !hasFamilies;
   }

      /**
      *
      * @param gedcom
      * @return if the person should be printed, false otherwise. This is
      * determined by whether the person is living and if he belongs to
      * any families that have dead people in them
      **/
   public boolean shouldPrint(Gedcom gedcom)
   {
      return (shouldAlwaysPrint || isIncluded(gedcom));
   }

   public boolean isIncluded(Gedcom gedcom) {
      return (!isLiving() && !isEmpty() && !isBeforeCutoff(gedcom));
   }

   // Title which are directly attached to the INDI tag in the GEDCOM
   // They will later be attached to the appropriate name
   private List<Name.Title> titles = new ArrayList<Name.Title>();

   /**
    * Add name title attached directly to an INDI tag
    * @param title
    */
   public void addTitle(Name.Title title) {
      titles.add(title);
   }

   /**
    * We have finished parsing the INDI tag for this
    * person. This method appends all of the titles
    * that are attached directly to the person
    * to the primary name of the person as prefixes.
    *
    * Also, it adds any citations attached to such
    * titles to the primary name.
    * @param ged
    */
   public void end(Gedcom ged) {
      if (name != null)
      {
         for (Name.Title title: titles)
         {
            name.appendPrefix(title.getTitle());
            name.addCitations(title.getCitations());
            addNoteNotes(title.getNotes());
            addNoteNoteCitations(title.getNoteCitations());
         }
      } else
      {
         logger.info(ged.logStr("You have no name but a title in person"));
      }
   }

   private boolean primary = false;

   public void setPrimary()
   {
      primary = true;
   }

   public void findProblems()
   {
      String birthDate = null;
      String deathDate = null;
      String burialDate = null;
      List <EventDate> nonBirthStdDates = new ArrayList<EventDate>();              // data type changed from String on this and next line Oct 2021 by Janet Bjorndahl
      List <EventDate> nonDeathProbateStdDates = new ArrayList<EventDate>();

      // Let's find potential problems for this person.
      for (Event event : getEvents())
      {
         if (event.getType() != Event.Type.lds_baptism &&
             event.getType() != Event.Type.lds_blessing &&
             event.getType() != Event.Type.lds_confirmation &&
             event.getType() != Event.Type.lds_ordination &&
             event.getType() != Event.Type.lds_endowment &&
             event.getType() != Event.Type.lds_child_sealing) {
            String date = event.getAttribute("DATE");
            if (!Utils.isEmpty(date)) {
               // Edit dates - error if the date cannot be interpreted; otherwise an alert if the date requires signficant reformating. Added Aug 2021 by Janet Bjorndahl
               EventDate eventDate = new EventDate(date, event.eventType());
               if (eventDate.editDate()) {
                  if (eventDate.getSignificantReformat()) {
                     addProblem("0" + event.eventType() + " date automatically reformated from \"" + date + "\" to \"" + eventDate.getFormatedDate() + "\"");
                  }
               }
               else {
                  addProblem("2" + eventDate.getErrorMessage() + ": " + date + " (Please write dates in \"d mmm yyyy\" format, e.g., 5 Jan 1900)");
               }
               
               if (event.getType() != Event.Type.Probate &&
                     event.getType() != Event.Type.death &&
                     event.getType() != Event.Type.alt_death &&
                     event.getType() != Event.Type.burial &&
                     event.getType() != Event.Type.Will &&
                     !(event.getType() == Event.Type.Other && "Alt. Death".equalsIgnoreCase(event.getDescription()))) {
                  nonDeathProbateStdDates.add(eventDate);                          // variable changed from sortDate Oct 2021 by Janet Bjorndahl
               }
               if (event.getType() != Event.Type.birth &&
                     event.getType() != Event.Type.alt_birth &&
                     !(event.getType() == Event.Type.Other && "Alt. Birth".equalsIgnoreCase(event.getDescription()))) {
                  nonBirthStdDates.add(eventDate);                                 // variable changed from sortDate Oct 2021 by Janet Bjorndahl
               }
               switch (event.getType())
               {
                  case birth:
                     birthDate = date;
                     break;
                  case death:
                     deathDate = date;
                  case burial:
                     burialDate = date;
                     break;
               }
            }
         }
      }

      int minDeathDay = new EventDate(deathDate).getMinDay();            // method replaced in these 6 rows Oct 2021 by Janet Bjorndahl
      int maxDeathDay = new EventDate(deathDate).getMaxDay();
      int minBurialDay = new EventDate(burialDate).getMinDay();
      int maxBurialDay = new EventDate(burialDate).getMaxDay();
      int minBirthDay = new EventDate(birthDate).getMinDay();
      int maxBirthDay = new EventDate(birthDate).getMaxDay();

      if (minDeathDay > 0 && maxBirthDay > 0 && minDeathDay - maxBirthDay > 115*365) {
         addProblem("2Death is more than 115 years after birth");
      }
      if (minBirthDay > 0 && maxDeathDay > 0 && maxDeathDay < minBirthDay) {
         addProblem("2Birth is after death");
      }
      if (minBurialDay > 0 && maxDeathDay > 0 && minBurialDay - maxDeathDay > 30) {
         addProblem("0Burial is more than 30 days after death");
      }
      if (maxBurialDay > 0 && minDeathDay > 0 && maxBurialDay < minDeathDay) {
         addProblem("2Death is after burial");
      }

      if (minBirthDay > 0) {
         for (EventDate otherDate : nonBirthStdDates) {                // data type changed from String Oct 2021 by Janet Bjorndahl
            int otherDay = otherDate.getMaxDay();                      // method replaced Oct 2021 by Janet Bjorndahl
            if (otherDay > 0 && otherDay < minBirthDay) {
               addProblem("2An event occurs before birth");
               break;
            }
         }
      }

      if (maxDeathDay > 0) {
         for (EventDate otherDate : nonDeathProbateStdDates)           // data type changed from String Oct 2021 by Janet Bjorndahl
         {
            int otherDay = otherDate.getMinDay();                      // method replaced Oct 2021 by Janet Bjorndahl
            if (otherDay != 0 && otherDay - maxDeathDay > 365)
            {
               addProblem("0An event occurs more than a year after death");
               break;
            }
         }
      }

      // Let's see if the person is missing a gender:
      if (gender == Gender.unknown && name != null) {
         String givenName = name.getGiven();
         if (!Utils.isEmpty(givenName)) {
            givenName = givenName.toLowerCase();
            if (!givenName.equals("?") &&
                givenName.indexOf("stillborn") < 0 &&
                givenName.indexOf("child") < 0 &&
                givenName.indexOf("baby") < 0 &&
                givenName.indexOf("unknown") < 0) {
               addProblem("0Missing gender");
            }
         }
      }

      if (name != null && !Utils.isEmpty(name.getSurname()) && name.getSurname().trim().length() == 1 &&
            !name.getSurname().trim().equals("?"))
      {
         addProblem("0Surname has only one letter");
      }
   }

   /**
    * Prints the person out to the XML file via PrintWriter out
    * @param gedcom
    * @param out
    * @param encodeXML
    * @throws Uploader.PrintException
    * @throws Gedcom.PostProcessException
    */
   public void print(Gedcom gedcom, PrintWriter out, boolean encodeXML)
         throws Uploader.PrintException, Gedcom.PostProcessException
   {
      startPage(out, gedcom, primary, !shouldPrint(gedcom), isLiving(), isBeforeCutoff(gedcom));
      try
      {
         StringBuffer buf = new StringBuffer();
         StringBuffer sourceBuffer = new StringBuffer();
         StringBuffer noteBuffer = new StringBuffer();
         buf.append("<person>\n");
         if (getName() != null)
         {
            getName().print(buf, sourceBuffer, noteBuffer, this, gedcom);
         } else
         {
            (new Name()).print(buf, sourceBuffer, noteBuffer, this, gedcom);
         }
         // Now let's print the alternate names:
         printAltNames(buf, sourceBuffer, noteBuffer, gedcom);
         if (getGender() != Gender.unknown)
         {
            buf.append(Uploader.printTag("gender", getGenderString()));
         } else
         {
            buf.append(Uploader.printTag("gender", "?"));
         }
         StringBuffer bodyText = new StringBuffer();
         if (!Utils.isEmpty(primaryChildOf))
         {
            Family fam = gedcom.getFamilies().get(primaryChildOf);
            if (fam != null)
            {
               fam.printChildOfFamily(buf);
            } else
            {
               logger.info("Primary child of family id \"" + primaryChildOf + "\" is not valid");
               bodyText.append("Missing primary child of family with GEDCOM ID: " + primaryChildOf);
            }
         }

         for (String famID1 : getChildOfFamilies())
         {
            if (!famID1.equals(primaryChildOf))
            {
               Family fam1 = gedcom.getFamilies().get(famID1);
               if (fam1 != null)
               {
                  fam1.printChildOfFamily(buf);
               } else{
                  logger.info ("When printing person " + getID() +
                        ", child of family id: \"" + famID1 + "\" is not valid");
                  Utils.prependParagraphBreak(bodyText);
                  bodyText.append("Missing child of family with GEDCOM ID: " + famID1);
               }
            }
         }

         for (String famID : getSpouseOfFamilies())
         {
            Family fam = gedcom.getFamilies().get(famID);
            if (fam != null)
            {
               GedcomElementWriter ew = new GedcomElementWriter("spouse_of_family");
               ew.put("id", fam.getID());
               ew.write(buf);
            } else
            {
               logger.info("When printing person " + getID() +
                     ", spouse of family id: \"" + famID + "\" is not valid");
               Utils.prependParagraphBreak(bodyText);
               bodyText.append("Missing spouse of family with GEDCOM ID: " + famID);
            }
         }
         if (!Utils.isEmpty(pedi) && !Utils.isEmpty(pediID))
         {
            Family fam = gedcom.getFamilies().get(pediID);
            if (fam != null)
            {
               bodyText.append(pedi).append(" parents: ").append(fam.getWikiTitle(gedcom));
            } else throw new Uploader.PrintException("PEDI family id: \"" + pediID + "\" is not valid", gedcom, getID());
         }

         for (String todoID : getTodos())
         {
            String todo = gedcom.getTodos().get(todoID);
            if (!Utils.isEmpty(todo))
            {
               addNote(todo);
            } else
            {
               throw new Uploader.PrintException("Reference to non-existant TODO object", gedcom, getID());
            }
         }
         printNotes(bodyText, getCitations(), gedcom);
         if (!Utils.isEmpty(bodyText.toString()))
         {
            bodyText.append("\n");
         }
         bodyText.append("<show_sources_images_notes/>\n");
         printEvents(gedcom, buf, sourceBuffer, noteBuffer);
         printCitations(sourceBuffer, noteBuffer, gedcom);
         buf.append(sourceBuffer);
         buf.append(noteBuffer);
         buf.append("</person>\n");
         buf.append(Utils.replaceHTMLFormatting(bodyText));
         // We want to finish printing the images
         // Finish printing out the content
         out.print("<content>");
         String outText = buf.toString();
         if (encodeXML)
         {
            //outText = Utils.encodeXML(outText);
            outText = encloseInCDATA(outText);
         }
         out.print(outText);
      } catch (Uploader.PrintException e)
      {
         logger.warn(e);
      }
      out.println("</content>");
      out.println("</page>");
   }

   // Prints the alternate names attached to this person
   private void printAltNames(StringBuffer buf, StringBuffer sourceBuffer,
                              StringBuffer noteBuffer,
                              Gedcom gedcom)
         throws Uploader.PrintException, Gedcom.PostProcessException
   {
      for (AlternateName an : getAltNames())
      {
         an.print(buf, sourceBuffer, noteBuffer, this, gedcom);
      }
   }

   // Prints an LDS event that is attached to this person,
   // (and belongs in the person's page data element
   private void printLDSEvent(Event event, Gedcom gedcom, PrintWriter out)
         throws Uploader.PrintException, Gedcom.PostProcessException
   {
      String endString = "/>\n";
      switch (event.getType())
      {
         case lds_baptism:
            event.printWholeLDSEvent("BAPL", gedcom, out, this);
            break;
         case lds_endowment:
            event.printWholeLDSEvent("ENDL", gedcom, out, this);
            break;
         case lds_child_sealing:
            String famc = event.getAttribute("FAMC");
            if (!Utils.isEmpty(famc))
            {
               boolean foundFamily = false;
               for (String childOfFamily : this.getChildOfFamilies())
               {
                  if (childOfFamily.equals(famc))
                  {
                     foundFamily = true;
                     break;
                  }
               }
               if (!foundFamily)
               {
                  logger.info(gedcom.logStr("FAMC \"" + famc + "\" for SLGC in person not found"));
               }
            }
            event.printWholeLDSEvent("SLGC", gedcom, out, this);
            break;
         default:
            // Do nothing
      }
   }

   public String getBirthDate() {
      return getBirthDate(true);
   }

   // Searches for and returns a date for a birth.
   // Tries to find a birth event with a date first,
   // and if it can't find one, then it looks for
   // a christening event with a date.
   public String getBirthDate(boolean useChristening) {
      for (Event event : getEvents())
      {
         if (event.getType() == Event.Type.birth &&
               !Utils.isEmpty(event.getAttribute("DATE")))
         {
            return event.getAttribute("DATE");
         }
      }

      if (useChristening) {
         for (Event event : getEvents())
         {
            if ((event.getType() == Event.Type.christening || event.getType() == Event.Type.Baptism) &&
                  !Utils.isEmpty(event.getAttribute("DATE")))
            {
               return event.getAttribute("DATE");
            }
         }
      }

      return null;
   }

   // Searches for and returns a date for a death.
   // Tries to find a birth event with a date first,
   // and if it can't find one, then it looks for
   // a burial event with a date.
   public String getDeathDate () {
      for (Event event : getEvents())
      {
         if (event.getType() == Event.Type.death &&
               !Utils.isEmpty(event.getAttribute("DATE")))
         {
            return event.getAttribute("DATE");
         }
      }

      for (Event event : getEvents())
      {
         if (event.getType() == Event.Type.burial &&
               !Utils.isEmpty(event.getAttribute("DATE")))
         {
            return event.getAttribute("DATE");
         }
      }

      return null;
   }

   public Set<Citation> getAllCitations(Gedcom gedcom) throws Gedcom.PostProcessException
   {
      Set<Citation> citations = new HashSet<Citation>();
      citations.addAll(getCitations());
      for (Note note : getNotesFromCitations(gedcom)) citations.addAll(note.getSourceCitations());
      Name name = getName();
      if (name != null) {
         citations.addAll(name.getCitations());
         for (Note note : name.getNotes()) citations.addAll(note.getSourceCitations());
         for (Note note : Note.Citation.getNotesFromCitations(gedcom, name.getNoteCitations())) citations.addAll(note.getSourceCitations());
      }
      for (AlternateName altName : getAltNames()) {
         citations.addAll(altName.getCitations());
         for (Note note : altName.getNotes()) citations.addAll(note.getSourceCitations());
         for (Note note : Note.Citation.getNotesFromCitations(gedcom, altName.getNoteCitations())) citations.addAll(note.getSourceCitations());
      }
      for (Event event : getEvents()) {
         citations.addAll(event.getCitations());
         for (Note note : event.getNotes()) citations.addAll(note.getSourceCitations());
         for (Note note : Note.Citation.getNotesFromCitations(gedcom, event.getNoteCitations())) citations.addAll(note.getSourceCitations());
      }
      return citations;
   }
}
