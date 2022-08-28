(ns heraldicon.heraldry.coat-of-arms
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.heraldry.field.render :as field.render]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.options :as options]
   [heraldicon.render.hatching :as hatching]
   [heraldicon.render.outline :as outline]
   [heraldicon.render.pattern :as pattern]
   [heraldicon.render.texture :as texture]
   [heraldicon.svg.filter :as filter]
   [heraldicon.svg.metadata :as svg.metadata]
   [heraldicon.svg.squiggly :as squiggly]
   [heraldicon.util.uid :as uid]))

(derive :heraldry/coat-of-arms :heraldry.options/root)

(defmethod interface/options-subscriptions :heraldry/coat-of-arms [_context]
  #{})

(defmethod interface/options :heraldry/coat-of-arms [_context]
  {:manual-blazon options/manual-blazon})

(defmethod interface/blazon-component :heraldry/coat-of-arms [context]
  (interface/blazon (-> context
                        (c/++ :field)
                        (assoc-in [:blazonry :root?] true))))

(defmethod interface/properties :heraldry/coat-of-arms [{:keys [target-width]
                                                         :as context}]
  (let [target-width (or target-width 100)
        escutcheon (interface/render-option :escutcheon context)
        flag-width (interface/render-option :flag-width context)
        flag-height (interface/render-option :flag-height context)
        flag-swallow-tail (interface/render-option :flag-swallow-tail context)
        flag-tail-point-height (interface/render-option :flag-tail-point-height context)
        flag-tail-tongue (interface/render-option :flag-tail-tongue context)
        escutcheon-data (escutcheon/data escutcheon flag-width flag-height flag-swallow-tail
                                         flag-tail-point-height flag-tail-tongue)]
    {:type :heraldry/coat-of-arms
     :escutcheon-data (escutcheon/transform-to-width escutcheon-data target-width)}))

(defmethod interface/environment :heraldry/coat-of-arms [_context {:keys [escutcheon-data]}]
  (:environment escutcheon-data))

(defmethod interface/render-shape :heraldry/coat-of-arms [context {:keys [escutcheon-data]}]
  (let [squiggly? (interface/render-option :squiggly? context)]
    {:shape [(cond-> (:shape escutcheon-data)
               squiggly? squiggly/squiggly-path)]}))

(defmethod interface/bounding-box :heraldry/coat-of-arms [_context {:keys [escutcheon-data]}]
  (:shape-bounding-box escutcheon-data))

(defmethod interface/exact-shape :heraldry/coat-of-arms [context _properties]
  (-> (interface/get-render-shape context) :shape first))

(defmethod interface/render-component :heraldry/coat-of-arms [{:keys
                                                               [svg-export?
                                                                metadata-path
                                                                texture-link] :as context}]
  (let [{:keys [min-x min-y]
         :as bounding-box} (interface/get-bounding-box context)
        [width height] (bb/size bounding-box)
        shape-paths (:shape (interface/get-render-shape context))
        mode (interface/render-option :mode context)
        escutcheon-shadow? (when-not svg-export?
                             (interface/render-option :escutcheon-shadow? context))
        escutcheon-outline? (interface/render-option :escutcheon-outline? context)
        outline? (interface/render-option :outline? context)
        shiny? (interface/render-option :shiny? context)
        theme (interface/render-option :theme context)
        texture (interface/render-option :texture context)
        texture-displacement? (interface/render-option :texture-displacement? context)
        mask-id (uid/generate "mask")
        texture-id (uid/generate "texture")
        shiny-id (uid/generate "shiny")
        texture-link (or texture-link
                         (texture/full-path texture))]
    [:g {:filter (when escutcheon-shadow?
                   "url(#shadow)")}
     (when metadata-path
       [svg.metadata/attribution (assoc context :path metadata-path) :arms])
     [:defs
      (when shiny?
        [:filter {:id shiny-id}
         [:feDiffuseLighting {:in "SourceGraphic"
                              :result "light"
                              :lighting-color "white"}
          [:fePointLight {:x 75
                          :y 20
                          :z 20}]]
         [:feComposite {:in "SourceGraphic"
                        :in2 "light"
                        :operator "arithmetic"
                        :k1 1
                        :k2 0
                        :k3 0
                        :k4 0}]])
      (when texture-link
        [:filter {:id texture-id}
         [:feImage {:href texture-link
                    :x min-x
                    :y min-y
                    :width (max width height)
                    :height (max width height)
                    :preserveAspectRatio "none"
                    :result "image"}]
         (when texture-displacement?
           [:feDisplacementMap {:in "SourceGraphic"
                                :in2 "image"
                                :scale (texture/displacement texture)
                                :xChannelSelector "R"
                                :yChannelSelector "R"
                                :result "displaced"}])
         [:feComposite {:in (if texture-displacement?
                              "displaced"
                              "SourceGraphic")
                        :in2 "image"
                        :operator "arithmetic"
                        :k1 1
                        :k2 0
                        :k3 0
                        :k4 0}]])
      (when-not svg-export?
        filter/shadow)
      [pattern/defs theme]
      (when (= mode :hatching)
        hatching/defs)]
     [:defs
      [(if svg-export?
         :mask
         :clipPath)
       {:id mask-id}
       [:path {:d (s/join "" shape-paths)
               :clip-rule "evenodd"
               :fill-rule "evenodd"
               :fill "#fff"
               :stroke "none"}]]]
     [:g {(if svg-export?
            :mask
            :clip-path) (str "url(#" mask-id ")")}
      [:g {:filter (when texture-link (str "url(#" texture-id ")"))}
       [:g {:filter (when shiny?
                      (str "url(#" shiny-id ")"))}
        [:path {:d (s/join "" shape-paths)
                :fill-rule "evenodd"
                :fill "#f0f0f0"}]
        [field.render/render (-> (c/++ context :field)
                                 (dissoc :metadata-path))]]]]
     (when (or escutcheon-outline?
               outline?)
       [:g (outline/style context)
        [:path {:d (s/join "" shape-paths)}]])]))
