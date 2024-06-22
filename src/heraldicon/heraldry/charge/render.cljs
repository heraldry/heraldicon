(ns heraldicon.heraldry.charge.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.charge.other :as other]
   [heraldicon.heraldry.render :as render]
   [heraldicon.interface :as interface]
   [heraldicon.util.uid :as uid]))

(defmethod interface/render-component :heraldry/charge [context]
  (let [{:keys [svg-export?
                clip?
                self-below-shield?
                render-pass-below-shield?]} (c/render-hints context)
        shape-clip-path-id (uid/generate "clip")
        vertical-mask-clip-path-id (uid/generate "clip")
        {:keys [vertical-mask-shape
                other?]
         :as properties} (interface/get-properties context)]
    [:<>
     (when vertical-mask-shape
       [:defs
        [(if (and svg-export?
                  (not clip?))
           :mask
           :clipPath) {:id vertical-mask-clip-path-id}
         [:path {:d vertical-mask-shape
                 :clip-rule "evenodd"
                 :fill-rule "evenodd"
                 :fill "#fff"}]]])
     [:g (when vertical-mask-shape
           {(if (and svg-export?
                     (not clip?))
              :mask
              :clip-path) (str "url(#" vertical-mask-clip-path-id ")")})
      (if other?
        [other/render context properties]
        (when (= (boolean self-below-shield?)
                 (boolean render-pass-below-shield?))
          [:<>
           [:defs
            [(if (and svg-export?
                      (not clip?))
               :mask
               :clipPath) {:id shape-clip-path-id}
             [render/shape-mask context]]]
           [render/shape-fimbriation context]
           [:g {(if (and svg-export?
                         (not clip?))
                  :mask
                  :clip-path) (str "url(#" shape-clip-path-id ")")}
            [interface/render-component (c/++ context :field)]]
           [render/charge-edges context]
           [render/shape-highlight context]]))]]))
