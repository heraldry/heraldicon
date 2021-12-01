(ns heraldry.coat-of-arms.texture
  (:require
   [heraldry.config :as config]
   [heraldry.gettext :refer [string]]
   [heraldry.util :as util]))

(def textures
  [[(string "None") :none nil 0]
   [(string "Cloth (rough)") :cloth-rough "/textures/cloth-rough.jpg" 1]
   [(string "Cloth (smooth)") :cloth-smooth "/textures/cloth-smooth.jpg" 1]
   [(string "Felt") :felt "/textures/felt.jpg" 1]
   [(string "Glass (frosted)") :glass-frosted "/textures/glass-frosted.jpg" 1.5]
   [(string "Marble") :marble "/textures/marble.jpg" 1]
   [(string "Metal (brushed 1)") :metal-brushed-1 "/textures/metal-brushed-1.jpg" 1]
   [(string "Metal (brushed 2)") :metal-brushed-2 "/textures/metal-brushed-2.jpg" 1]
   [(string "Metal (rough)") :metal-rough "/textures/metal-rough.jpg" 1]
   [(string "Metal (scraped)") :metal-scraped "/textures/metal-scraped.jpg" 1]
   [(string "Metal (smooth)") :metal-smooth "/textures/metal-smooth.jpg" 1]
   [(string "Mosaic") :mosaic "/textures/mosaic.jpg" 2]
   [(string "Satin") :satin "/textures/satin.jpg" 10]
   [(string "Stone") :stone "/textures/stone.jpg" 1]
   [(string "Stone (rough)") :stone-rough "/textures/stone-rough.jpg" 1]
   [(string "Wood") :wood "/textures/wood.jpg" 1]])

(def choices
  (->> textures
       (map (fn [[display-name key _path _displacement]]
              [display-name key]))
       vec))

(def texture-map
  (util/choices->map choices))

(def relative-paths
  (->> textures
       (map (fn [[_display-name key path _displacement]]
              [key path]))
       (into {})))

(def displacements
  (->> textures
       (map (fn [[_display-name key _path displacement]]
              [key displacement]))
       (into {})))

(defn displacement [texture]
  (some->> texture
           (get displacements)))

(defn full-path [texture]
  (some->> texture
           (get relative-paths)
           (str (config/get :heraldry-url))))
