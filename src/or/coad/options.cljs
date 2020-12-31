(ns or.coad.options
  (:require [clojure.walk :as walk]))

(defn defaults [options-function type]
  (-> type
      options-function
      (walk/postwalk #(or (:default %) %))))

(defn contains-in?
  [m ks]
  (not= ::absent (get-in m ks ::absent)))

(defn get-options [path options]
  (let [type (->> options
                  (filter #(contains-in? % path))
                  (map #(get-in % (conj path :type)))
                  first)
        default (->> options
                     (filter #(contains-in? % path))
                     (map #(get-in % (conj path :default)))
                     first)]
    (-> {:type type
         :default default}
        (cond->
         (= type :choice) (->
                           (assoc :choices (->> options
                                                (filter #(contains-in? % path))
                                                (map #(get-in % (conj path :choices)))
                                                first)))
         (= type :range) (->
                          (assoc :min (->> options
                                           (filter #(contains-in? % path))
                                           (map #(get-in % (conj path :min)))
                                           (apply max)))
                          (assoc :max (->> options
                                           (filter #(contains-in? % path))
                                           (map #(get-in % (conj path :max)))
                                           (apply min))))))))

(defn get-value [path value options]
  (let [real-options (get-options path options)
        value (or value
                  (:default real-options))]
    (case (:type real-options)
      :range (if (nil? value)
               nil
               (let [min-value (:min real-options)
                     max-value (:max real-options)]
                 (-> value
                     (max min-value)
                     (min max-value))))
      value)))

#_{:clj-kondo/ignore [:redefined-var]}
(defn merge
  [x other]
  (let [x (if (nil? x) {} x)]
    (into x
          (for [[key value] other]
            (cond
              (nil? value) [key nil]
              (map? value) [key (merge (get x key) value)]
              :else [key value])))))

#_(merge {:origin {:point {:type :choice
                           :choices [["Fess" :fess]
                                     ["Chief" :chief]]
                           :default :fess}
                   :offset-x {:type :range
                              :min -45
                              :max 45
                              :default 0}
                   :offset-y {:type :range
                              :min -45
                              :max 45
                              :default 0}}
          :diagonal-mode {:type :choice
                          :choices [["foo" :foo]]}
          :line {:style {:choices [["Straight" :straight]
                                   ["Invected" :invected]]
                         :default :straight}
                 :eccentricity {:type :range
                                :min 10
                                :max 20}}}
         {:origin {:offset-x nil}
          :diagonal-mode nil
          :line {:eccentricity {:max 4}}
          :new-option {:type :range
                       :min 5}})
