(ns heraldry.options
  (:require
   [clojure.set :as set]
   [clojure.walk :as walk]
   [heraldry.gettext :refer [string]]
   [heraldry.util :as util]))

(def option-types #{:range :choice :boolean :text})

(def manual-blazon
  {:type :text
   :default nil
   :ui {:label (string "Manual blazon")}})

(def plain-outline?-option
  {:type :boolean
   :default false
   :ui {:label (string "Outline")}})

(defn get-value [value options]
  (if (and (vector? value)
           (-> value first (= :force)))
    (second value)
    (let [fallback (first (keep identity [(:inherited options)
                                          (:default options)]))
          value (first (keep identity [value
                                       fallback]))]
      (case (:type options)
        :boolean (boolean value)
        :choice (let [choices (util/choices->map (:choices options))]
                  (if (contains? choices value)
                    value
                    (if (contains? choices fallback)
                      fallback
                      (-> options :choices first second))))
        :range (cond
                 (and (nil? value)
                      (-> options :default nil?)) nil
                 (nil? value) (:min options)
                 :else (-> value
                           (max (:min options))
                           (min (:max options))))
        :text (or value (:default options))
        nil))))

(defn get-sanitized-value-or-nil [value options]
  (if (nil? value)
    nil
    (case (:type options)
      :boolean (boolean value)
      :choice (let [choices (util/choices->map (:choices options))]
                (if (contains? choices value)
                  value
                  nil))
      :range (if (nil? value)
               nil
               (-> value
                   (max (:min options))
                   (min (:max options))))
      :text value
      nil)))

(defn sanitize [values given-options]
  (into {}
        (for [[k v] given-options]
          (cond
            (and (map? v)
                 (not (contains?
                       option-types (:type v)))) [k (sanitize (get values k) v)]
            :else [k (get-value (get values k) v)]))))

(defn sanitize-value-or-data [data options]
  (when (map? options)
    ;; TODO: find better way to determine option leaf or branch
    (if (or (-> options :type not)
            (-> options :type :type))
      (sanitize data options)
      (get-value data options))))

(defn remove-nil-values-and-empty-maps [m]
  (walk/postwalk #(if (map? %)
                    (into {} (filter (fn [[_ v]] (not (or (nil? v)
                                                          (and (map? v)
                                                               (empty? v))))) %))
                    %)
                 m))

(defn sanitize-or-nil [values given-options]
  (-> {}
      (into (for [[k v] given-options]
              (cond
                (and (map? v)
                     (not (contains?
                           option-types (:type v)))) [k (sanitize-or-nil (get values k) v)]
                :else [k (get-sanitized-value-or-nil (get values k) v)])))
      remove-nil-values-and-empty-maps))

(defn pick [opts paths & values]
  (let [values (first values)
        options (loop [options {}
                       [path & rest] paths]
                  (let [next-options (-> options
                                         (assoc-in path (get-in opts path)))]
                    (if (nil? rest)
                      next-options
                      (recur next-options rest))))]
    (loop [options options
           [[key value] & rest] values]
      (let [next-options (if key
                           (assoc-in options key value)
                           options)]
        (if (nil? rest)
          next-options
          (recur next-options rest))))))

(defn override-if-exists [options path value]
  (if (get-in options path)
    (assoc-in options path value)
    options))

(defn populate-inheritance [options inherited-values]
  (->> options
       (map (fn [[k v]]
              [k
               (if (and (-> v :type #{:choice :range :boolean :text})
                        (get inherited-values k))
                 (assoc v :inherited (get inherited-values k))
                 v)]))
       (into {})))

(defn changed? [key sanitized-data options]
  (and (get options key)
       (not= (get sanitized-data key)
             (or (-> options (get key) :inherited)
                 (-> options (get key) :default)))))

(def ordinary-options-subscriptions
  #{[:type]
    [:line]
    [:line :type]
    [:line :fimbriation]
    [:line :fimbriation :mode]
    [:opposite-line :type]
    [:opposite-line :fimbriation :mode]
    [:extra-line :type]
    [:extra-line :fimbriation :mode]

    [:cottising :cottise-1 :line]
    [:cottising :cottise-1 :line :type]
    [:cottising :cottise-1 :line :fimbriation]
    [:cottising :cottise-1 :line :fimbriation :mode]
    [:cottising :cottise-1 :opposite-line :type]
    [:cottising :cottise-1 :opposite-line :fimbriation :mode]
    [:cottising :cottise-1 :extra-line :type]
    [:cottising :cottise-1 :extra-line :fimbriation :mode]

    [:cottising :cottise-2 :line]
    [:cottising :cottise-2 :line :type]
    [:cottising :cottise-2 :line :fimbriation]
    [:cottising :cottise-2 :line :fimbriation :mode]
    [:cottising :cottise-2 :opposite-line :type]
    [:cottising :cottise-2 :opposite-line :fimbriation :mode]
    [:cottising :cottise-2 :extra-line :type]
    [:cottising :cottise-2 :extra-line :fimbriation :mode]

    [:cottising :cottise-opposite-1 :line]
    [:cottising :cottise-opposite-1 :line :type]
    [:cottising :cottise-opposite-1 :line :fimbriation]
    [:cottising :cottise-opposite-1 :line :fimbriation :mode]
    [:cottising :cottise-opposite-1 :opposite-line :type]
    [:cottising :cottise-opposite-1 :opposite-line :fimbriation :mode]
    [:cottising :cottise-opposite-1 :extra-line :type]
    [:cottising :cottise-opposite-1 :extra-line :fimbriation :mode]

    [:cottising :cottise-opposite-2 :line]
    [:cottising :cottise-opposite-2 :line :type]
    [:cottising :cottise-opposite-2 :line :fimbriation]
    [:cottising :cottise-opposite-2 :line :fimbriation :mode]
    [:cottising :cottise-opposite-2 :opposite-line :type]
    [:cottising :cottise-opposite-2 :opposite-line :fimbriation :mode]
    [:cottising :cottise-opposite-2 :extra-line :type]
    [:cottising :cottise-opposite-2 :extra-line :fimbriation :mode]

    [:cottising :cottise-extra-1 :line]
    [:cottising :cottise-extra-1 :line :type]
    [:cottising :cottise-extra-1 :line :fimbriation]
    [:cottising :cottise-extra-1 :line :fimbriation :mode]
    [:cottising :cottise-extra-1 :opposite-line :type]
    [:cottising :cottise-extra-1 :opposite-line :fimbriation :mode]
    [:cottising :cottise-extra-1 :extra-line :type]
    [:cottising :cottise-extra-1 :extra-line :fimbriation :mode]

    [:cottising :cottise-extra-2 :line]
    [:cottising :cottise-extra-2 :line :type]
    [:cottising :cottise-extra-2 :line :fimbriation]
    [:cottising :cottise-extra-2 :line :fimbriation :mode]
    [:cottising :cottise-extra-2 :opposite-line :type]
    [:cottising :cottise-extra-2 :opposite-line :fimbriation :mode]
    [:cottising :cottise-extra-2 :extra-line :type]
    [:cottising :cottise-extra-2 :extra-line :fimbriation :mode]

    [:origin :point]
    [:direction-anchor :point]
    [:anchor :point]

    [:fimbriation :mode]
    [:geometry :size-mode]
    [:distance]
    [:thickness]
    [:voided :voided?]
    [:humetty :humetty?]})

(def charge-options-subscriptions
  #{[:type]
    [:escutcheon]
    [:anchor :point]
    [:variant]
    [:data]
    [:fimbriation :mode]})

(def charge-group-options-subscriptions
  #{[:type]})

(def semy-options-subscriptions
  #{})

(def motto-options-subscriptions
  #{[:type]
    [:ribbon-variant]})

(def shared-options-subscriptions
  (set/union
   ordinary-options-subscriptions
   charge-options-subscriptions
   charge-group-options-subscriptions
   semy-options-subscriptions
   motto-options-subscriptions))
