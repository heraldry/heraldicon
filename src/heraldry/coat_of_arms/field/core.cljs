(ns heraldry.coat-of-arms.field.core
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.field.interface :as field-interface]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.interface :as interface]
            [heraldry.options :as options]
            [heraldry.util :as util]))

(defn mandatory-part-count [path context]
  (let [field-type (-> (interface/get-raw-data (conj path :type) context)
                       name keyword)
        num-base-fields (interface/get-sanitized-data (conj path :num-base-fields) context)]
    (if num-base-fields
      num-base-fields
      (case field-type
        nil 0
        :tierced-per-pale 3
        :tierced-per-fess 3
        :tierced-per-pairle 3
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
         :per-pile} type) (into (subvec defaults 0 2)
                                [(nth defaults 2)])
      :else (subvec defaults 0 2))))

(defn part-name [field-type index]
  (-> field-type field-interface/part-names (get index) (or (util/to-roman (inc index)))))

(defn title [path context]
  (if (interface/get-sanitized-data (conj path :counterchanged?) context)
    {:en "Counterchanged"
     :de "Verwechselt"}
    (let [field-type (interface/get-sanitized-data (conj path :type) context)]
      (if (= field-type :heraldry.field.type/plain)
        (-> (interface/get-raw-data (conj path :tincture) context)
            util/translate-tincture
            util/upper-case-first)
        (get field-options/field-map field-type)))))

(defmethod interface/blazon-component :heraldry.component/field [path context]
  (if (interface/get-sanitized-data (conj path :counterchanged?) context)
    "counterchanged"
    (let [root? (get-in context [:blazonry :root?])
          context (assoc-in context [:blazonry :root?] false)
          field-type (interface/get-sanitized-data (conj path :type) context)
          num-components (interface/get-list-size (conj path :components) context)

          field-description
          (case field-type
            :heraldry.field.type/plain (util/translate-tincture
                                        (interface/get-sanitized-data (conj path :tincture) context))
            (let [line (interface/get-sanitized-data (conj path :line) context)
                  mandatory-part-count (mandatory-part-count path context)
                  num-fields (interface/get-list-size (conj path :fields) context)]
              (util/combine
               " "
               [(util/translate field-type)
                (util/translate-line line)
                (util/combine " and "
                              (->> (range num-fields)
                                   (map
                                    (fn [index]
                                      (let [subfield-path (conj path :fields index)]
                                        (cond
                                          (< index mandatory-part-count) (interface/blazon subfield-path context)
                                          (not= (interface/get-raw-data (conj subfield-path :type) context)
                                                :heraldry.field.type/ref) (util/combine
                                                                           " "
                                                                           [(when (> num-fields 3)
                                                                              (str (part-name field-type index) ":"))
                                                                            (interface/blazon subfield-path context)])))))))])))
          components-description (util/combine
                                  ", "
                                  (map (fn [index]
                                         (interface/blazon (conj path :components index) context))
                                       (range num-components)))

          blazon (-> (util/combine ", " [field-description
                                         components-description])
                     util/upper-case-first)]
      (if (or root?
              (and (= field-type :heraldry.field.type/plain)
                   (zero? num-components)))
        blazon
        (str "[" blazon "]")))))
