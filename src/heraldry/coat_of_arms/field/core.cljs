(ns heraldry.coat-of-arms.field.core
  (:require [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.field.options :as division-options]
            [heraldry.coat-of-arms.field.type.barry :as barry]
            [heraldry.coat-of-arms.field.type.bendy :as bendy]
            [heraldry.coat-of-arms.field.type.bendy-sinister :as bendy-sinister]
            [heraldry.coat-of-arms.field.type.chequy :as chequy]
            [heraldry.coat-of-arms.field.type.gyronny :as gyronny]
            [heraldry.coat-of-arms.field.type.lozengy :as lozengy]
            [heraldry.coat-of-arms.field.type.masonry :as masonry]
            [heraldry.coat-of-arms.field.type.paly :as paly]
            [heraldry.coat-of-arms.field.type.papellony :as papellony]
            [heraldry.coat-of-arms.field.type.per-bend :as per-bend]
            [heraldry.coat-of-arms.field.type.per-bend-sinister :as per-bend-sinister]
            [heraldry.coat-of-arms.field.type.per-chevron :as per-chevron]
            [heraldry.coat-of-arms.field.type.per-fess :as per-fess]
            [heraldry.coat-of-arms.field.type.per-pale :as per-pale]
            [heraldry.coat-of-arms.field.type.per-pile :as per-pile]
            [heraldry.coat-of-arms.field.type.per-saltire :as per-saltire]
            [heraldry.coat-of-arms.field.type.plain :as plain]
            [heraldry.coat-of-arms.field.type.potenty :as potenty]
            [heraldry.coat-of-arms.field.type.quartered :as quartered]
            [heraldry.coat-of-arms.field.type.quarterly :as quarterly]
            [heraldry.coat-of-arms.field.type.tierced-per-fess :as tierced-per-fess]
            [heraldry.coat-of-arms.field.type.tierced-per-pairle :as tierced-per-pairle]
            [heraldry.coat-of-arms.field.type.tierced-per-pairle-reversed :as tierced-per-pairle-reversed]
            [heraldry.coat-of-arms.field.type.tierced-per-pale :as tierced-per-pale]
            [heraldry.coat-of-arms.field.type.vairy :as vairy]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.core :as ordinary]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.util :as util]))

(defn mandatory-part-count [{:keys [type] :as division}]
  (let [{:keys [layout]} (options/sanitize division (division-options/options division))]
    (if (:num-base-fields layout)
      (:num-base-fields layout)
      (case type
        nil                          0
        :tierced-per-pale            3
        :tierced-per-fess            3
        :tierced-per-pairle          3
        :tierced-per-pairle-reversed 3
        :tierced-per-pile            3
        2))))

(defn default-fields [{:keys [type] :as division}]
  (let [{:keys [layout]}          (options/sanitize division (division-options/options division))
        {:keys [num-fields-x
                num-fields-y
                num-base-fields]} layout
        defaults                  [default/field
                                   (-> default/field
                                       (assoc :tincture :azure))
                                   (-> default/field
                                       (assoc :tincture :sable))
                                   (-> default/field
                                       (assoc :tincture :gules))
                                   (-> default/field
                                       (assoc :tincture :or))
                                   (-> default/field
                                       (assoc :tincture :vert))]]
    (cond
      (= :per-saltire type)     (-> (subvec defaults 0 2)
                                    (into [{:ref 1} {:ref 0}]))
      (= :quartered type)       (-> (subvec defaults 0 2)
                                    (into [{:ref 1} {:ref 0}]))
      (= :quarterly type)       (-> (subvec defaults 0 2)
                                    (into (map (fn [i]
                                                 (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                    (into (->> (for [j (range num-fields-y)
                                                     i (range num-fields-x)]
                                                 {:ref (mod (+ i j) num-base-fields)})
                                               (drop num-base-fields))))
      (= :gyronny type)         (-> (subvec defaults 0 2)
                                    (into [{:ref 1} {:ref 0} {:ref 0} {:ref 1} {:ref 1} {:ref 0}]))
      (= :paly type)            (if (= num-fields-y 1)
                                  (subvec defaults 0 1)
                                  (-> (subvec defaults 0 2)
                                      (into (map (fn [i]
                                                   (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                      (into (map (fn [i]
                                                   {:ref (mod i num-base-fields)}) (range (- num-fields-x num-base-fields))))))
      (= :barry type)           (if (= num-fields-y 1)
                                  (subvec defaults 0 1)
                                  (-> (subvec defaults 0 2)
                                      (into (map (fn [i]
                                                   (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                      (into (map (fn [i]
                                                   {:ref (mod i num-base-fields)}) (range (- num-fields-y num-base-fields))))))
      (= :chequy type)          (if (= [num-fields-x num-fields-y] [1 1])
                                  (subvec defaults 0 1)
                                  (-> (subvec defaults 0 2)
                                      (into (map (fn [i]
                                                   (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))))
      (#{:vairy
         :potenty
         :papellony
         :masonry} type)        [(-> default/field
                                     (assoc :tincture :azure))
                                 (-> default/field
                                     (assoc :tincture :argent))]
      (#{:bendy
         :bendy-sinister} type) (-> (subvec defaults 0 2)
                                    (into (map (fn [i]
                                                 (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                    (into (map (fn [i]
                                                 {:ref (mod i num-base-fields)}) (range (- num-fields-y num-base-fields)))))
      (#{:tierced-per-pale
         :tierced-per-fess
         :tierced-per-pairle
         :tierced-per-pairle-reversed
         :per-pile} type)       (into (subvec defaults 0 2)
                                      [(nth defaults 2)])
      :else                     (subvec defaults 0 2))))

(def fields
  [#'plain/render
   #'per-pale/render
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
   #'per-pile/render
   #'paly/render
   #'barry/render
   #'bendy/render
   #'bendy-sinister/render
   #'chequy/render
   #'lozengy/render
   #'vairy/render
   #'potenty/render
   #'papellony/render
   #'masonry/render])

(def kinds-function-map
  (->> fields
       (map (fn [function]
              [(-> function meta :value) function]))
       (into {})))

(def choices
  (->> fields
       (map (fn [function]
              [(-> function meta :display-name) (-> function meta :value)]))))

(def field-map
  (util/choices->map choices))

(defn part-name [type index]
  (let [function (get kinds-function-map type)]
    (-> function meta :parts (get index) (or (util/to-roman (inc index))))))

(defn render [{:keys [type components] :as field} environment
              {:keys
               [db-path fn-component-selected?
                fn-select-component svg-export? transform] :as context}]
  (let [selected?       (when fn-component-selected?
                          (fn-component-selected? db-path))
        context         (-> context
                            (assoc :render-field render))
        render-function (get kinds-function-map type)]
    [:<>
     [:g {:on-click  (when fn-select-component
                       (fn [event]
                         (fn-select-component db-path)
                         (.stopPropagation event)))
          :style     (when (not svg-export?)
                       {:pointer-events "visiblePainted"
                        :cursor         "pointer"})
          :transform transform}
      [render-function field environment context]
      (for [[idx element] (map-indexed vector components)]
        (if (-> element :component (= :ordinary))
          ^{:key idx} [ordinary/render element field environment (-> context
                                                                     (update :db-path conj :components idx))]
          ^{:key idx} [charge/render element field environment (-> context
                                                                   (update :db-path conj :components idx))]))]
     (when selected?
       [:path {:d     (:shape environment)
               :style {:opacity 0.25}
               :fill  "url(#selected)"}])]))

