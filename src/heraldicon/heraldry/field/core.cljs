(ns heraldicon.heraldry.field.core
  (:require
   [heraldicon.blazonry :as blazonry]
   [heraldicon.context :as c]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.options :as field.options]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.util.number :as number]))

(defn mandatory-part-count [context]
  (let [field-type (-> (interface/get-raw-data (c/++ context :type))
                       name keyword)
        num-base-fields (interface/get-sanitized-data (c/++ context :num-base-fields))]
    (or num-base-fields
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
                  (assoc default/field :tincture :azure)
                  (assoc default/field :tincture :sable)
                  (assoc default/field :tincture :gules)
                  (assoc default/field :tincture :or)
                  (assoc default/field :tincture :vert)
                  (assoc default/field :tincture :purpure)
                  (assoc default/field :tincture :tenne)
                  (assoc default/field :tincture :sanguine)
                  (assoc default/field :tincture :rose)
                  (assoc default/field :tincture :murrey)
                  (assoc default/field :tincture :bleu-celeste)]]
    (cond
      (#{:plain :counterchanged} type) []
      (#{:per-saltire
         :quartered} type) (into (subvec defaults 0 2)
                                 [{:type :heraldry.field.type/ref
                                   :index 1}
                                  {:type :heraldry.field.type/ref
                                   :index 0}])
      (= :quarterly type) (let [effective-num-base-fields (min (* num-fields-x num-fields-y) num-base-fields)]
                            (into []
                                  (for [j (range num-fields-y)
                                        i (range num-fields-x)]
                                    (let [idx (+ (* j num-fields-x) i)]
                                      (if (< idx effective-num-base-fields)
                                        (nth defaults idx)
                                        {:type :heraldry.field.type/ref
                                         :index (mod (+ i j) effective-num-base-fields)})))))
      (= :gyronny type) (into (subvec defaults 0 2)
                              [{:type :heraldry.field.type/ref
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
                                :index 0}])
      (#{:gyronny-n
         :paly} type) (into []
                            (map (fn [i]
                                   (if (< i num-base-fields)
                                     (nth defaults i)
                                     {:type :heraldry.field.type/ref
                                      :index (mod i num-base-fields)})))
                            (range num-fields-x))
      (#{:barry
         :chevronny
         :bendy
         :bendy-sinister} type) (into []
                                      (map (fn [i]
                                             (if (< i num-base-fields)
                                               (nth defaults i)
                                               {:type :heraldry.field.type/ref
                                                :index (mod i num-base-fields)})))
                                      (range num-fields-y))
      (= :chequy type) (subvec defaults 0 num-base-fields)
      (#{:vairy
         :potenty
         :papellony
         :masony} type) [(assoc default/field :tincture :azure)
                         (assoc default/field :tincture :argent)]
      (#{:tierced-per-pale
         :tierced-per-fess
         :tierced-per-pall
         :per-pile} type) (subvec defaults 0 3)
      :else (subvec defaults 0 2))))

(defn default-fields [context]
  (let [type (interface/get-raw-data (c/++ context :type))
        num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        num-base-fields (interface/get-sanitized-data (c/++ context :layout :num-base-fields))]
    (raw-default-fields type num-fields-x num-fields-y num-base-fields)))

(defn part-name [field-type index]
  (-> field-type field.interface/part-names (get index) (or (number/to-roman (inc index)))))

(defn title [context]
  (let [field-type (interface/get-sanitized-data (c/++ context :type))]
    (if (= field-type :heraldry.field.type/plain)
      (tincture/translate-tincture
       (interface/get-sanitized-data (c/++ context :tincture)))
      (get field.options/field-map field-type))))

(defmethod interface/blazon-component :heraldry/field [context]
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
            (string/combine
             " "
             [(blazonry/translate (if (= field-type :heraldry.field.type/gyronny-n)
                                    :heraldry.field.type/gyronny
                                    field-type))
              (blazonry/translate-line line)
              (string/combine " and "
                              (->> (range num-fields)
                                   (map
                                    (fn [index]
                                      (let [subfield-context (c/++ context :fields index)]
                                        (cond
                                          (< index mandatory-part-count) (interface/blazon subfield-context)
                                          (not= (interface/get-raw-data (c/++ subfield-context :type))
                                                :heraldry.field.type/ref) (string/combine
                                                                           " "
                                                                           [(when (> num-fields 3)
                                                                              (string/str-tr (part-name field-type index) ":"))
                                                                            (interface/blazon subfield-context)])))))))])))
        components-description (string/combine
                                ", "
                                (map (fn [index]
                                       (interface/blazon (c/++ context :components index)))
                                     (range num-components)))

        blazon (string/upper-case-first (string/combine ", " [field-description
                                                              components-description]))]
    (if (or root?
            (and (= field-type :heraldry.field.type/plain)
                 (zero? num-components)))
      blazon
      (string/str-tr "(" blazon ")"))))

(defmethod interface/environment :heraldry/field [context _properties]
  (interface/get-effective-parent-environment context))

(defmethod interface/exact-shape :heraldry/field [context _properties]
  (interface/get-effective-parent-shape context))

(defmethod interface/render-shape :heraldry/field [context _properties]
  (interface/get-render-shape (interface/parent context)))
