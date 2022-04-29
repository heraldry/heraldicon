(ns heraldicon.heraldry.field.core
  (:require
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.options :as field.options]
   [heraldicon.heraldry.tincture.core :as tincture]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.util :as util]))

(defn mandatory-part-count [context]
  (let [field-type (-> (interface/get-raw-data (c/++ context :type))
                       name keyword)
        num-base-fields (interface/get-sanitized-data (c/++ context :num-base-fields))]
    (if num-base-fields
      num-base-fields
      (case field-type
        nil 0
        :tierced-per-pale 3
        :tierced-per-fess 3
        :tierced-per-pall 3
        :per-pile 3
        2))))

(defn raw-default-fields [type num-fields-x num-fields-y num-base-fields]
  (let [type (-> type name keyword)
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
                      (assoc :tincture :vert))
                  (-> default/field
                      (assoc :tincture :purpure))
                  (-> default/field
                      (assoc :tincture :tenne))
                  (-> default/field
                      (assoc :tincture :sanguine))
                  (-> default/field
                      (assoc :tincture :rose))
                  (-> default/field
                      (assoc :tincture :murrey))
                  (-> default/field
                      (assoc :tincture :bleu-celeste))]]
    (cond
      (#{:plain :counterchanged} type) []
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
      (= :gyronny-n type) (into (subvec defaults 0 num-base-fields)
                                (->> (for [i (range num-fields-x)]
                                       {:type :heraldry.field.type/ref
                                        :index (mod i num-base-fields)})
                                     (drop num-base-fields)))
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
      (= :chevronny type) (if (= num-fields-y 1)
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
         :masony} type) [(-> default/field
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
         :tierced-per-pall
         :per-pile} type) (into (subvec defaults 0 2)
                                [(nth defaults 2)])
      :else (subvec defaults 0 2))))

(defn default-fields [context]
  (let [type (interface/get-raw-data (c/++ context :type))
        num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        num-base-fields (interface/get-sanitized-data (c/++ context :layout :num-base-fields))]
    (raw-default-fields type num-fields-x num-fields-y num-base-fields)))

(defn part-name [field-type index]
  (-> field-type field.interface/part-names (get index) (or (util/to-roman (inc index)))))

(defn title [context]
  (let [field-type (interface/get-sanitized-data (c/++ context :type))]
    (if (= field-type :heraldry.field.type/plain)
      (-> (interface/get-sanitized-data (c/++ context :tincture))
          tincture/translate-tincture)
      (get field.options/field-map field-type))))

(defmethod interface/blazon-component :heraldry.component/field [context]
  (let [root? (get-in context [:blazonry :root?])
        context (assoc-in context [:blazonry :root?] false)
        field-type (interface/get-sanitized-data (c/++ context :type))
        num-components (interface/get-list-size (c/++ context :components))

        field-description
        (case field-type
          :heraldry.field.type/plain (tincture/translate-tincture
                                      (interface/get-sanitized-data (c/++ context :tincture)))
          (let [line (interface/get-sanitized-data (c/++ context :line))
                mandatory-part-count (mandatory-part-count context)
                num-fields (interface/get-list-size (c/++ context :fields))]
            (util/combine
             " "
             [(util/translate (if (= field-type :heraldry.field.type/gyronny-n)
                                :heraldry.field.type/gyronny
                                field-type))
              (util/translate-line line)
              (util/combine " and "
                            (->> (range num-fields)
                                 (map
                                  (fn [index]
                                    (let [subfield-context (c/++ context :fields index)]
                                      (cond
                                        (< index mandatory-part-count) (interface/blazon subfield-context)
                                        (not= (interface/get-raw-data (c/++ subfield-context :type))
                                              :heraldry.field.type/ref) (util/combine
                                                                         " "
                                                                         [(when (> num-fields 3)
                                                                            (util/str-tr (part-name field-type index) ":"))
                                                                          (interface/blazon subfield-context)])))))))])))
        components-description (util/combine
                                ", "
                                (map (fn [index]
                                       (interface/blazon (c/++ context :components index)))
                                     (range num-components)))

        blazon (-> (util/combine ", " [field-description
                                       components-description])
                   util/upper-case-first)]
    (if (or root?
            (and (= field-type :heraldry.field.type/plain)
                 (zero? num-components)))
      blazon
      (util/str-tr "(" blazon ")"))))