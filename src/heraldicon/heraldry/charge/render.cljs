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
        horizontal-mask-clip-path-id (uid/generate "clip")
        {:keys [vertical-mask-shape
                horizontal-mask-shape
                other?]
         :as properties} (interface/get-properties context)
        mask-or-clip-path (if (and svg-export? (not clip?)) :mask :clipPath)
        mask-or-clip-path-attr (if (and svg-export? (not clip?)) :mask :clip-path)]
    [:<>
     (when vertical-mask-shape
       [:defs
        [mask-or-clip-path {:id vertical-mask-clip-path-id}
         [:path {:d vertical-mask-shape
                 :clip-rule "evenodd"
                 :fill-rule "evenodd"
                 :fill "#fff"}]]])
     (when horizontal-mask-shape
       [:defs
        [mask-or-clip-path {:id horizontal-mask-clip-path-id}
         [:path {:d horizontal-mask-shape
                 :clip-rule "evenodd"
                 :fill-rule "evenodd"
                 :fill "#fff"}]]])
     [:g (when vertical-mask-shape
           {mask-or-clip-path-attr (str "url(#" vertical-mask-clip-path-id ")")})
      [:g (when horizontal-mask-shape
            {mask-or-clip-path-attr (str "url(#" horizontal-mask-clip-path-id ")")})
       (if other?
         [other/render context properties]
         (when (= (boolean self-below-shield?)
                  (boolean render-pass-below-shield?))
           [:<>
            [:defs
             [mask-or-clip-path {:id shape-clip-path-id}
              [render/shape-mask context]]]
            [render/shape-fimbriation context]
            [:g {mask-or-clip-path-attr (str "url(#" shape-clip-path-id ")")}
             [interface/render-component (c/++ context :field)]]
            [render/charge-edges context]
            [render/shape-highlight context]]))]]]))
