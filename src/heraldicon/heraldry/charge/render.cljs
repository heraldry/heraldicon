(ns heraldicon.heraldry.charge.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.charge.other :as other]
   [heraldicon.heraldry.render :as render]
   [heraldicon.interface :as interface]
   [heraldicon.render.ir :as ir]
   [heraldicon.util.uid :as uid]))

(defn- render-ir [context]
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
        mask-or-clip-path-attr (if (and svg-export? (not clip?)) :mask :clip-path)
        charge-ir (cond
                    other? (other/render-ir context properties)
                    (= (boolean self-below-shield?)
                       (boolean render-pass-below-shield?)) (-> ir/blank
                                                                (ir/add-def [mask-or-clip-path {:id shape-clip-path-id}
                                                                             [render/shape-mask context]])
                                                                (assoc :body
                                                                       [:<>
                                                                        [render/shape-fimbriation context]
                                                                        [:g {mask-or-clip-path-attr (str "url(#" shape-clip-path-id ")")}
                                                                         [interface/render-component (c/++ context :field)]]
                                                                        [render/charge-edges context]
                                                                        [render/shape-highlight context]]))
                    :else ir/blank)]
    (-> charge-ir
        (cond->
          vertical-mask-shape (ir/add-def [mask-or-clip-path {:id vertical-mask-clip-path-id}
                                           [:path {:d vertical-mask-shape
                                                   :clip-rule "evenodd"
                                                   :fill-rule "evenodd"
                                                   :fill "#fff"}]])
          horizontal-mask-shape (ir/add-def [mask-or-clip-path {:id horizontal-mask-clip-path-id}
                                             [:path {:d horizontal-mask-shape
                                                     :clip-rule "evenodd"
                                                     :fill-rule "evenodd"
                                                     :fill "#fff"}]]))
        (update :body (fn [body]
                        [:g (when vertical-mask-shape
                              {mask-or-clip-path-attr (str "url(#" vertical-mask-clip-path-id ")")})
                         [:g (when horizontal-mask-shape
                               {mask-or-clip-path-attr (str "url(#" horizontal-mask-clip-path-id ")")})
                          body]])))))

(defmethod interface/render-component :heraldry/charge [context]
  (ir/->hiccup (render-ir context)))
