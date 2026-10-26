(ns heraldicon.localization.string
  (:require
   [clojure.string :as str]
   [heraldicon.localization.locale :as locale]))

;; translated text is only resolved for one language when it is displayed, so
;; only the selected language's dictionary needs to be loaded
(defrecord ^:private Joined [separator parts upper-case-first?]
  ;; React stringifies keys, and choice labels are used as keys
  Object
  (toString [this]
    (pr-str* this)))

(defn- upper-case-first-str [s]
  (let [s (or s "")]
    (str (str/upper-case (subs s 0 1)) (subs s 1))))

(defn tr-raw [data language]
  (cond
    (keyword? data) (let [translated (locale/string language data)]
                      (if (some-> translated count pos?)
                        translated
                        (locale/string :en data)))
    (instance? Joined data) (let [{:keys [separator parts upper-case-first?]} data]
                              (cond-> (->> parts
                                           (map (fn [s]
                                                  (tr-raw s language)))
                                           (filter (comp pos? count))
                                           (str/join (tr-raw separator language)))
                                upper-case-first? upper-case-first-str))
    (map? data) (let [translated (get data language)]
                  (if (some-> translated count pos?)
                    translated
                    (get data :en)))
    :else (str data)))

(defn- translatable? [data]
  (or (keyword? data)
      (map? data)))

(defn- join [separator parts]
  (let [joined (->Joined separator (vec parts) false)]
    (if (some translatable? (cons separator parts))
      joined
      (tr-raw joined :en))))

(defn combine [separator words]
  (join separator words))

(defn str-tr [& strs]
  (join "" strs))

(defn- interleave-all
  "Returns a lazy seq of the first item in each coll, then the second etc.,
   unlike the clojure.core version this includes all elements."
  [c1 c2]
  (lazy-seq
   (let [s1 (seq c1) s2 (seq c2)]
     (when (or s1 s2)
       (cond->> (interleave-all (rest s1) (rest s2))
         (first s2) (cons (first s2))
         (first s1) (cons (first s1)))))))

(defn format-tr [s & args]
  (let [chunks (str/split s "%s")]
    (apply str-tr (interleave-all chunks args))))

(defn upper-case-first [s]
  (if (translatable? s)
    (->Joined "" [s] true)
    (upper-case-first-str s)))
