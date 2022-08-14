(ns heraldicon.heraldry.field.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.render :as render]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.util.uid :as uid]))

(declare render)

(defn- render-subfield [{:keys [svg-export?]
                         :as context} render-components]
  (let [clip-path-id (uid/generate "clip")]
    [:g
     [:defs
      [(if svg-export?
         :mask
         :clipPath) {:id clip-path-id}
       ;; TODO: edge overlap strategy
       [render/shape-mask context]]]
     [:g {(if svg-export?
            :mask
            :clip-path) (str "url(#" clip-path-id ")")}
      [render context render-components]]]))

(defn- render-subfields [context render-components]
  (let [{:keys [num-subfields]} (interface/get-properties context)]
    (into [:g]
          (map (fn [idx]
                 [render-subfield (c/++ context :fields idx) render-components]))
          (range num-subfields))))

(defn render [context render-components]
  (let [{field-type :type} (interface/get-properties context)]
    [:<>
     (case field-type
       :heraldry.field.type/plain (tincture/tinctured-field context)
       [render-subfields context render-components])

     [render/field-edges context]

     [render-components context]]))
