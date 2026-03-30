(ns heraldicon.util.date)

(def ^:private day-of-week
  {"1" "Mon" "2" "Tue" "3" "Wed" "4" "Thu"
   "5" "Fri" "6" "Sat" "0" "Sun"})

(def ^:private month-name
  {"01" "Jan" "02" "Feb" "03" "Mar" "04" "Apr"
   "05" "May" "06" "Jun" "07" "Jul" "08" "Aug"
   "09" "Sep" "10" "Oct" "11" "Nov" "12" "Dec"})

(defn iso->rfc822
  "Converts ISO date string (YYYY-MM-DD) to RFC-822 format."
  [date-str]
  (let [d (js/Date. (str date-str "T00:00:00Z"))
        dow (day-of-week (str (.getUTCDay d)))
        day (.getUTCDate d)
        mon (month-name (let [m (inc (.getUTCMonth d))]
                          (if (< m 10) (str "0" m) (str m))))
        year (.getUTCFullYear d)]
    (str dow ", " (if (< day 10) (str "0" day) day) " " mon " " year " 00:00:00 GMT")))
