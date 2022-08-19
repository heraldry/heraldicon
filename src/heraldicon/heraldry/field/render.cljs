(ns heraldicon.heraldry.field.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.render :as render]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.util.uid :as uid]))

(declare render)

(defn- effective-field-context [context]
  (let [field-type (interface/get-raw-data (c/++ context :type))]
    (if (= field-type :heraldry.field.type/ref)
      (let [index (interface/get-raw-data (c/++ context :index))
            source-context (-> context c/-- (c/++ index))]
        (assoc-in source-context [:path-map (:path source-context)] (:path context)))
      context)))

(defn- render-subfield [{:keys [svg-export?]
                         :as context} render-components]
  (let [clip-path-id (uid/generate "clip")
        subfield-context (effective-field-context context)]
    [:g
     [:defs
      [(if svg-export?
         :mask
         :clipPath) {:id clip-path-id}
       ;; TODO: edge overlap strategy
       [render/shape-mask subfield-context]]]
     [:g {(if svg-export?
            :mask
            :clip-path) (str "url(#" clip-path-id ")")}
      [render subfield-context render-components]]]))

(defn- render-subfields [context render-components]
  (let [{:keys [num-subfields]} (interface/get-properties context)]
    (into [:g]
          (map (fn [idx]
                 [render-subfield (c/++ context :fields idx) render-components]))
          (range num-subfields))))

(defn render [context render-components]
  (let [{:keys [render-fn]
         field-type :type
         :as properties} (interface/get-properties context)]
    [:<>
     (cond
       (= field-type :heraldry.field.type/plain) (tincture/tinctured-field context)
       ;; TODO: simplify once render-components doesn't have to be passed along anymore
       render-fn [render-fn context properties]
       :else [render-subfields context render-components])

     [render/field-edges context]

     [render-components context]]))
