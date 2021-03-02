(ns heraldry.coat-of-arms.division.core
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.type.barry :as barry]
            [heraldry.coat-of-arms.division.type.bendy :as bendy]
            [heraldry.coat-of-arms.division.type.bendy-sinister :as bendy-sinister]
            [heraldry.coat-of-arms.division.type.chequy :as chequy]
            [heraldry.coat-of-arms.division.type.gyronny :as gyronny]
            [heraldry.coat-of-arms.division.type.lozengy :as lozengy]
            [heraldry.coat-of-arms.division.type.paly :as paly]
            [heraldry.coat-of-arms.division.type.per-bend :as per-bend]
            [heraldry.coat-of-arms.division.type.per-bend-sinister :as per-bend-sinister]
            [heraldry.coat-of-arms.division.type.per-chevron :as per-chevron]
            [heraldry.coat-of-arms.division.type.per-fess :as per-fess]
            [heraldry.coat-of-arms.division.type.per-pale :as per-pale]
            [heraldry.coat-of-arms.division.type.per-saltire :as per-saltire]
            [heraldry.coat-of-arms.division.type.quartered :as quartered]
            [heraldry.coat-of-arms.division.type.quarterly :as quarterly]
            [heraldry.coat-of-arms.division.type.tierced-per-fess :as tierced-per-fess]
            [heraldry.coat-of-arms.division.type.tierced-per-pairle :as tierced-per-pairle]
            [heraldry.coat-of-arms.division.type.tierced-per-pairle-reversed :as tierced-per-pairle-reversed]
            [heraldry.coat-of-arms.division.type.tierced-per-pale :as tierced-per-pale]
            [heraldry.coat-of-arms.division.type.vair :as vair]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.util :as util]))

(defn mandatory-part-count [{:keys [type] :as division}]
  (let [{:keys [num-base-fields]} (options/sanitize division (division-options/options division))]
    (if (get #{:paly :barry} type)
      num-base-fields
      (case type
        nil                          0
        :tierced-per-pale            3
        :tierced-per-fess            3
        :tierced-per-pairle          3
        :tierced-per-pairle-reversed 3
        2))))

(defn counterchangable? [division]
  ;; TODO: potentially also should look at the parts, maybe demand no
  ;; ordinaries and charges as well, but for now this check suffices
  (and (-> division mandatory-part-count (= 2))
       (-> division :fields (get 0) :division :type not)
       (-> division :fields (get 1) :division :type not)))

(defn default-fields [{:keys [type] :as division}]
  (let [{:keys [layout]}                                    (options/sanitize division (division-options/options division))
        {:keys [num-fields-x num-fields-y num-base-fields]} layout
        defaults                                            [default/field
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :azure))
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :sable))
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :gules))
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :or))
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :vert))]]
    (case type
      :vair [(-> default/field
                 (assoc-in [:content :tincture] :azure))
             (-> default/field
                 (assoc-in [:content :tincture] :argent))]
      (into (subvec defaults 0 2)
            (cond
              (= :per-saltire type)                  [{:ref 1} {:ref 0}]
              (= :quartered type)                    [{:ref 1} {:ref 0}]
              (= :quarterly type)                    (-> []
                                                         (into (map (fn [i]
                                                                      (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                                         (into (->> (for [j (range num-fields-y)
                                                                          i (range num-fields-x)]
                                                                      {:ref (mod (+ i j) num-base-fields)})
                                                                    (drop num-base-fields))))
              (= :gyronny type)                      [{:ref 1} {:ref 0} {:ref 0} {:ref 1} {:ref 1} {:ref 0}]
              (= :paly type)                         (-> []
                                                         (into (map (fn [i]
                                                                      (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                                         (into (map (fn [i]
                                                                      {:ref (mod i num-base-fields)}) (range (- num-fields-x num-base-fields)))))
              (= :barry type)                        (-> []
                                                         (into (map (fn [i]
                                                                      (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                                         (into (map (fn [i]
                                                                      {:ref (mod i num-base-fields)}) (range (- num-fields-y num-base-fields)))))
              (= :chequy type)                       (-> []
                                                         (into (map (fn [i]
                                                                      (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2)))))
              (#{:bendy
                 :bendy-sinister} type)              (-> []
                                                         (into (map (fn [i]
                                                                      (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                                         (into (map (fn [i]
                                                                      {:ref (mod i num-base-fields)}) (range (- num-fields-y num-base-fields)))))
              (#{:tierced-per-pale
                 :tierced-per-fess
                 :tierced-per-pairle
                 :tierced-per-pairle-reversed} type) [(nth defaults 2)])))))

(def divisions
  [#'per-pale/render
   #'per-fess/render
   #'per-bend/render
   #'per-bend-sinister/render
   #'per-chevron/render
   #'per-saltire/render
   #'quartered/render
   #'quarterly/render
   #'gyronny/render
   #'tierced-per-pale/render
   #'tierced-per-fess/render
   #'tierced-per-pairle/render
   #'tierced-per-pairle-reversed/render
   #'paly/render
   #'barry/render
   #'bendy/render
   #'bendy-sinister/render
   #'chequy/render
   #'lozengy/render
   #'vair/render])

(def kinds-function-map
  (->> divisions
       (map (fn [function]
              [(-> function meta :value) function]))
       (into {})))

(def choices
  (->> divisions
       (map (fn [function]
              [(-> function meta :display-name) (-> function meta :value)]))))

(def division-map
  (util/choices->map choices))

(defn part-name [type index]
  (let [function (get kinds-function-map type)]
    (-> function meta :parts (get index) (or (util/to-roman (inc index))))))

(defn render [{:keys [type] :as division} environment context]
  (let [function (get kinds-function-map type)]
    [function division environment context]))
