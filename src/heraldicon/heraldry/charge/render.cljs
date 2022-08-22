(ns heraldicon.heraldry.charge.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.render :as field.render]
   [heraldicon.heraldry.render :as render]
   [heraldicon.interface :as interface]
   [heraldicon.util.uid :as uid]))

(defmethod interface/render-component :heraldry/charge [{:keys [svg-export?]
                                                         :as context}]
  (let [clip-path-id (uid/generate "clip")
        {:keys [transform]} (interface/get-properties context)]
    [:g
     [:defs
      [(if svg-export?
         :mask
         :clipPath) {:id clip-path-id}
       [render/shape-mask context]]]
     #_[fimbriation context]
     [:g {(if svg-export?
            :mask
            :clip-path) (str "url(#" clip-path-id ")")}
      [:g (when transform
            {:transform transform})
       [field.render/render (c/++ context :field)]]]
     [render/charge-edges context]]))
