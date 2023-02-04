(ns heraldicon.heraldry.subfield
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.render :as render]
   [heraldicon.interface :as interface]
   [heraldicon.svg.path :as path]
   [heraldicon.util.uid :as uid]))

(derive :heraldry.subfield.type/field :heraldry.subfield/type)
(derive :heraldry.subfield.type/reference :heraldry.subfield/type)
(derive :heraldry.subfield/type :heraldry/subfield)

(def subfield-map
  {:heraldry.subfield.type/field :heraldry.subfield.type/field
   :heraldry.subfield.type/reference :heraldry.subfield.type/reference})

(defmethod interface/options :heraldry/subfield [_context]
  nil)

(defmethod interface/blazon-component :heraldry/subfield [context]
  (let [subfield-type (interface/get-raw-data (c/++ context :type))]
    (when (= subfield-type :heraldry.subfield.type/field)
      (interface/blazon-component (c/++ context :field)))))

(defmethod interface/properties :heraldry/subfield [_context]
  {:type :heraldry/subfield})

(defn- component-attribute [context get-attribute-fn]
  (let [subfield-environments (get-attribute-fn (interface/parent context))
        subfield-index (-> context :path last)]
    (get (:subfields subfield-environments) subfield-index)))

(defmethod interface/environment :heraldry/subfield [context]
  (component-attribute context interface/get-subfield-environments))

(defmethod interface/render-shape :heraldry/subfield [context]
  (component-attribute context interface/get-subfield-render-shapes))

(defmethod interface/exact-shape :heraldry/subfield [context]
  (let [{:keys [reverse-transform-fn]} (interface/get-properties (interface/parent context))
        shape-path (:shape (component-attribute context interface/get-subfield-render-shapes))
        shape-path (if (vector? shape-path)
                     (first shape-path)
                     shape-path)
        parent-shape (interface/get-parent-field-shape context)]
    (cond-> (environment/intersect-shapes shape-path parent-shape)
      reverse-transform-fn (->
                             path/parse-path
                             reverse-transform-fn
                             path/to-svg))))

(defn- effective-field-context [context]
  (let [subfield-type (interface/get-raw-data (c/++ context :type))]
    (-> (if (= subfield-type :heraldry.subfield.type/reference)
          (let [index (interface/get-raw-data (c/++ context :index))
                source-context (-> context c/-- (c/++ index))]
            (c/set-key source-context :path-redirect (:path context)))
          context)
        (c/++ :field))))

(defn render [context transform overlap?]
  (let [{:keys [charge-preview?
                svg-export?]} (c/render-hints context)
        clip-path-id (uid/generate "clip")
        field-context (effective-field-context context)]
    [:g
     [:defs
      [(if svg-export?
         :mask
         :clipPath) {:id clip-path-id}
       [render/shape-mask context overlap?]]]
     [:g {(if svg-export?
            :mask
            :clip-path) (str "url(#" clip-path-id ")")}
      [:g {:style (when-not (or svg-export?
                                charge-preview?)
                    {:pointer-events "visiblePainted"
                     :cursor "pointer"})
           :transform transform}
       [interface/render-component field-context]]]]))
