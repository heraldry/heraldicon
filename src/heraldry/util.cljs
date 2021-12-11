(ns heraldry.util
  (:require
   ["crypto" :as crypto]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as s]
   [clojure.walk :as walk]
   [goog.crypt :as crypt]
   [goog.crypt.base64 :as b64]
   [heraldry.config :as config]
   [heraldry.gettext :refer [known-languages]]
   [taoensso.timbre :as log]))

(def -current-id
  (atom 0))

(defn reset-id []
  (reset! -current-id 0))

(defn id [prefix]
  (str prefix "_" (swap! -current-id inc)))

(defn sha1 [data]
  (-> crypto
      (.createHash "sha1")
      (.update data)
      (.digest "hex")))

;; probably not safe to use with the path, as elements with the same path
;; could be rendered multiple times (e.g. charges in a charge-group)
(defn stable-id
  ([seed]
   (stable-id seed nil))

  ([seed suffix]
   (cond-> (-> seed pr-str sha1)
     suffix (str "-" suffix))))

(defn id-for-url [id]
  (when id
    (-> id
        (s/split #":" 2)
        second)))

(defn choices->map [choices]
  (->> choices
       (map (fn [[group-name & items]]
              (if (and (-> items count (= 1))
                       (-> items first keyword?))
                ;; in this case there is no group, treat the first element of "items" as key
                ;; and "group-name" as display-name
                [[(first items) group-name]]
                (->> items
                     (map (comp vec reverse))))))
       (apply concat)
       (into {})))

(defn filter-choices [choices pred]
  (let [pred (if (or (vector? pred)
                     (seq? pred))
               (set pred)
               pred)]
    (walk/postwalk (fn [v]
                     (cond
                       (and (vector? v)
                            (-> v count (= 2))
                            (-> v second keyword?)
                            (-> v second pred not)) nil
                       (and (vector? v)
                            (-> v count (= 2))
                            (-> v second vector?)
                            (-> v second count zero?)) nil
                       (vector? v) (filterv identity v)
                       :else v)) choices)))

(defn spy [value msg]
  (log/debug "spy:" msg)
  (pprint value)
  value)

(defn spy->> [msg value]
  (log/debug "spy:" msg)
  (pprint value)
  value)

(defn deep-merge-with [f & maps]
  (apply
   (fn m [& maps]
     (if (every? map? maps)
       (apply merge-with m maps)
       (apply f maps)))
   maps))

;; https://gist.github.com/jimweirich/1388782
(def roman-reductions
  '((1000 "M")
    (900 "CM") (500 "D") (400 "CD") (100 "C")
    (90 "XC") (50 "L") (40 "XL") (10 "X")
    (9 "IX") (5 "V") (4 "IV") (1 "I")))

(defn to-roman [number]
  (let [counts
        (map first
             (drop 1
                   (reductions (fn [[_ r] v]
                                 (list (int (/ r v))
                                       (- r (* v (int (/ r v))))))
                               (list 0 number)
                               (map first roman-reductions))))
        glyphs (map second roman-reductions)]
    (apply str
           (flatten
            (map (fn [[c g]] (take c (repeat g)))
                 (map vector counts glyphs))))))

(defn percent-of [base-value]
  (fn [v]
    (when v
      (-> v
          (* base-value)
          (/ 100)))))

(defn base64-decode-utf-8 [data]
  (-> data
      (b64/decodeStringToByteArray true)
      crypt/utf8ByteArrayToString))

(defn map-to-interval [value from to]
  (let [value (-> value
                  (max 0)
                  (min 1))]
    (-> (- to from)
        (* value)
        (+ from))))

(defn xor [a b]
  (or (and a (not b))
      (and (not a) b)))

(defn keyword->str [k]
  (-> k
      str
      (subs 1)))

;; https://gist.github.com/sebastibe/27be496c34ba6a3cce3b6425810a3dda
(defn vec-remove
  "Remove elem in coll by index."
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn vec-add
  "Add elem in coll by index."
  [coll pos el]
  (concat (subvec coll 0 pos) [el] (subvec coll pos)))

(defn vec-move
  "Move elem in coll by index"
  [coll pos1 pos2]
  (let [el (nth coll pos1)]
    (if (= pos1 pos2)
      coll
      (into [] (vec-add (vec-remove coll pos1) pos2 el)))))

(defn upper-case-first-str [s]
  (str (s/upper-case (or (first s) "")) (s/join (rest s))))

(defn upper-case-first [s]
  (if (map? s)
    (->> s
         (map (fn [[k v]]
                [k (upper-case-first-str v)]))
         (into {}))
    (upper-case-first-str s)))

(defn tr-raw [data language]
  (if (map? data)
    (let [translated (get data language)]
      (if (some-> translated count pos?)
        translated
        (get data :en)))
    data))

(defn translate [keyword]
  (when keyword
    (-> keyword
        name
        (s/replace "-" " ")
        (s/replace "fleur de lis" "fleur-de-lis")
        (s/replace "fleur de lys" "fleur-de-lys"))))

(defn translate-line [{:keys [type]}]
  (when (not= type :straight)
    (translate type)))

(defn translate-cap-first [keyword]
  (-> keyword
      translate
      upper-case-first))

(defn combine [separator words]
  (let [translated (->> known-languages
                        keys
                        (map (fn [language]
                               [language
                                (->> words
                                     (map (fn [s]
                                            (if (or (string? s)
                                                    (map? s))
                                              s
                                              (str s))))
                                     (map (fn [s]
                                            (tr-raw s language)))
                                     (filter #(> (count %) 0))
                                     (s/join (tr-raw separator language)))]))
                        (into {}))]
    (if (-> translated
            vals
            set
            count
            (= 1))
      (-> translated vals first)
      translated)))

(defn str-tr [& strs]
  (let [translated (->> known-languages
                        keys
                        (map (fn [language]
                               [language
                                (->> strs
                                     (map (fn [s]
                                            (if (or (string? s)
                                                    (map? s))
                                              s
                                              (str s))))
                                     (map (fn [s]
                                            (tr-raw s language)))
                                     (filter #(> (count %) 0))
                                     (apply str))]))
                        (into {}))]
    (if (-> translated
            vals
            set
            count
            (= 1))
      (-> translated vals first)
      translated)))

(defn interleave-all
  "Returns a lazy seq of the first item in each coll, then the second etc., unlike the clojure.core version this includes all elements."
  [c1 c2]
  (lazy-seq
   (let [s1 (seq c1) s2 (seq c2)]
     (when (or s1 s2)
       (cond->> (interleave-all (rest s1) (rest s2))
         (first s2) (cons (first s2))
         (first s1) (cons (first s1)))))))

(defn format-tr [s & args]
  (let [chunks (s/split s "%s")]
    (apply str-tr (interleave-all chunks args))))

(defn replace-recursively [data value replacement]
  (walk/postwalk #(if (= % value)
                    replacement
                    %)
                 data))

(defn matches-word [data word]
  (cond
    (keyword? data) (-> data name s/lower-case (s/includes? word))
    (string? data) (-> data s/lower-case (s/includes? word))
    (map? data) (some (fn [[k v]]
                        (or (and (keyword? k)
                                 (matches-word k word)
                                 ;; this would be an attribute entry, the value
                                 ;; must be truthy as well
                                 v)
                            (matches-word v word))) data)))

(defn short-url [arms-data]
  (if (= (config/get :stage) "prod")
    (let [{:keys [id version]} arms-data]
      (when (and id version)
        (if (zero? version)
          (str "https://coa.to/" (id-for-url id))
          (str "https://coa.to/" (id-for-url id) "/" version))))
    "https://dev"))

(defn index-of [item coll]
  (count (take-while (partial not= item) coll)))
