(ns heraldry.coat-of-arms.field.core
  (:require [heraldry.coat-of-arms.charge-group.core :as charge-group]
            [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.field.interface :as interface]
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
            [heraldry.coat-of-arms.semy.core :as semy]
            [heraldry.frontend.util :as frontend-util]
            [heraldry.util :as util]
            [heraldry.coat-of-arms.field.shared :as shared]))

(defn mandatory-part-count [{:keys [type] :as field}]
  (let [type (-> type name keyword)
        {:keys [layout]} (options/sanitize field (field-options/options field))]
    (if (:num-base-fields layout)
      (:num-base-fields layout)
      (case type
        nil 0
        :tierced-per-pale 3
        :tierced-per-fess 3
        :tierced-per-pairle 3
        :tierced-per-pairle-reversed 3
        :tierced-per-pile 3
        2))))

(defn default-fields [{:keys [type] :as field}]
  (let [type (-> type name keyword)
        {:keys [layout]} (options/sanitize field (field-options/options field))
        {:keys [num-fields-x
                num-fields-y
                num-base-fields]} layout
        defaults [default/field
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
      (= :per-saltire type) (-> (subvec defaults 0 2)
                                (into [{:type :heraldry.field.type/ref
                                        :index 1}
                                       {:type :heraldry.field.type/ref
                                        :index 0}]))
      (= :quartered type) (-> (subvec defaults 0 2)
                              (into [{:type :heraldry.field.type/ref
                                      :index 1}
                                     {:type :heraldry.field.type/ref
                                      :index 0}]))
      (= :quarterly type) (-> (subvec defaults 0 2)
                              (into (map (fn [i]
                                           (nth defaults (mod (+ i 2) (count defaults))))
                                         (range (- num-base-fields 2))))
                              (into (->> (for [j (range num-fields-y)
                                               i (range num-fields-x)]
                                           {:type :heraldry.field.type/ref
                                            :index (mod (+ i j) num-base-fields)})
                                         (drop num-base-fields))))
      (= :gyronny type) (-> (subvec defaults 0 2)
                            (into [{:type :heraldry.field.type/ref
                                    :index 1}
                                   {:type :heraldry.field.type/ref
                                    :index 0}
                                   {:type :heraldry.field.type/ref
                                    :index 0}
                                   {:type :heraldry.field.type/ref
                                    :index 1}
                                   {:type :heraldry.field.type/ref
                                    :index 1}
                                   {:type :heraldry.field.type/ref
                                    :index 0}]))
      (= :paly type) (if (= num-fields-y 1)
                       (subvec defaults 0 1)
                       (-> (subvec defaults 0 2)
                           (into (map (fn [i]
                                        (nth defaults (mod (+ i 2) (count defaults))))
                                      (range (- num-base-fields 2))))
                           (into (map (fn [i]
                                        {:type :heraldry.field.type/ref
                                         :index (mod i num-base-fields)})
                                      (range (- num-fields-x num-base-fields))))))
      (= :barry type) (if (= num-fields-y 1)
                        (subvec defaults 0 1)
                        (-> (subvec defaults 0 2)
                            (into (map (fn [i]
                                         (nth defaults (mod (+ i 2) (count defaults))))
                                       (range (- num-base-fields 2))))
                            (into (map (fn [i]
                                         {:type :heraldry.field.type/ref
                                          :index (mod i num-base-fields)})
                                       (range (- num-fields-y num-base-fields))))))
      (= :chequy type) (if (= [num-fields-x num-fields-y] [1 1])
                         (subvec defaults 0 1)
                         (-> (subvec defaults 0 2)
                             (into (map (fn [i]
                                          (nth defaults (mod (+ i 2) (count defaults))))
                                        (range (- num-base-fields 2))))))
      (#{:vairy
         :potenty
         :papellony
         :masonry} type) [(-> default/field
                              (assoc :tincture :azure))
                          (-> default/field
                              (assoc :tincture :argent))]
      (#{:bendy
         :bendy-sinister} type) (-> (subvec defaults 0 2)
                                    (into (map (fn [i]
                                                 (nth defaults (mod (+ i 2) (count defaults))))
                                               (range (- num-base-fields 2))))
                                    (into (map (fn [i]
                                                 {:type :heraldry.field.type/ref
                                                  :index (mod i num-base-fields)})
                                               (range (- num-fields-y num-base-fields)))))
      (#{:tierced-per-pale
         :tierced-per-fess
         :tierced-per-pairle
         :tierced-per-pairle-reversed
         :per-pile} type) (into (subvec defaults 0 2)
                                [(nth defaults 2)])
      :else (subvec defaults 0 2))))

(def fields
  [plain/field-type
   per-pale/field-type
   per-fess/field-type
   per-bend/field-type
   per-bend-sinister/field-type
   per-chevron/field-type
   per-saltire/field-type
   quartered/field-type
   quarterly/field-type
   gyronny/field-type
   tierced-per-pale/field-type
   tierced-per-fess/field-type
   tierced-per-pairle/field-type
   tierced-per-pairle-reversed/field-type
   per-pile/field-type
   paly/field-type
   barry/field-type
   bendy/field-type
   bendy-sinister/field-type
   chequy/field-type
   lozengy/field-type
   vairy/field-type
   potenty/field-type
   papellony/field-type
   ;; masonry/field-type
   ])

(def choices
  (->> fields
       (map (fn [key]
              [(interface/display-name key) key]))))

(def field-map
  (util/choices->map choices))

(defn part-name [field-type index]
  (-> field-type interface/part-names (get index) (or (util/to-roman (inc index)))))

(defn title [field]
  (let [sanitized-field (options/sanitize field (field-options/options field))]
    (str
     (if (-> field :type (= :heraldry.field.type/plain))
       (-> sanitized-field
           :tincture
           frontend-util/translate-tincture
           frontend-util/upper-case-first)
       (get field-map (:type field)))
     " field")))

;; TODO: should go away when not needed anymore
(def render shared/render)
