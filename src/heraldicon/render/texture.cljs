(ns heraldicon.render.texture
  (:require
   [heraldicon.config :as config]
   [heraldicon.options :as options]))

(def ^:private textures
  [[:string.render-options.texture-choice/none :none nil 0]
   [:string.render-options.texture-choice/cloth-rough :cloth-rough "/textures/cloth-rough.jpg" 1]
   [:string.render-options.texture-choice/cloth-smooth1 :cloth-smooth "/textures/cloth-smooth.jpg" 1]
   [:string.render-options.texture-choice/felt :felt "/textures/felt.jpg" 1]
   [:string.render-options.texture-choice/glass-frosted :glass-frosted "/textures/glass-frosted.jpg" 1.5]
   [:string.render-options.texture-choice/marble :marble "/textures/marble.jpg" 1]
   [:string.render-options.texture-choice/metal-brushed-1 :metal-brushed-1 "/textures/metal-brushed-1.jpg" 1]
   [:string.render-options.texture-choice/metal-brushed-2 :metal-brushed-2 "/textures/metal-brushed-2.jpg" 1]
   [:string.render-options.texture-choice/metal-rough :metal-rough "/textures/metal-rough.jpg" 1]
   [:string.render-options.texture-choice/metal-scraped :metal-scraped "/textures/metal-scraped.jpg" 1]
   [:string.render-options.texture-choice/metal-smooth :metal-smooth "/textures/metal-smooth.jpg" 1]
   [:string.render-options.texture-choice/mosaic :mosaic "/textures/mosaic.jpg" 2]
   [:string.render-options.texture-choice/satin :satin "/textures/satin.jpg" 10]
   [:string.render-options.texture-choice/stone :stone "/textures/stone.jpg" 1]
   [:string.render-options.texture-choice/stone-rough :stone-rough "/textures/stone-rough.jpg" 1]
   [:string.render-options.texture-choice/wood :wood "/textures/wood.jpg" 1]])

(def choices
  (map (fn [[display-name key _path _displacement]]
         [display-name key])
       textures))

(def texture-map
  (options/choices->map choices))

(def relative-paths
  (into {}
        (map (fn [[_display-name key path _displacement]]
               [key path]))
        textures))

(def ^:private displacements
  (into {}
        (map (fn [[_display-name key _path displacement]]
               [key displacement]))
        textures))

(defn displacement [texture]
  (get displacements texture))

(defn full-path [texture]
  (some->> (get relative-paths texture)
           (str (config/get :static-files-url))))
