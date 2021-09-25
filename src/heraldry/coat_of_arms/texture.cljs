(ns heraldry.coat-of-arms.texture
  (:require [heraldry.config :as config]
            [heraldry.strings :as strings]
            [heraldry.util :as util]))

(def textures
  [[strings/none :none nil 0]
   [{:en "Cloth (rough)"
     :de "Stoff (rau)"} :cloth-rough "/textures/cloth-rough.jpg" 1]
   [{:en "Cloth (smooth)"
     :de "Stoff (glatt)"} :cloth-smooth "/textures/cloth-smooth.jpg" 1]
   [{:en "Felt"
     :de "Filz"} :felt "/textures/felt.jpg" 1]
   [{:en "Glass (frosted)"
     :de "Glas (matt)"} :glass-frosted "/textures/glass-frosted.jpg" 1.5]
   [{:en "Marble"
     :de "Marmor"} :marble "/textures/marble.jpg" 1]
   [{:en "Metal (brushed 1)"
     :de "Metall (gebürstet 1)"} :metal-brushed-1 "/textures/metal-brushed-1.jpg" 1]
   [{:en "Metal (brushed 2)"
     :de "Metall (gebürstet 2)"} :metal-brushed-2 "/textures/metal-brushed-2.jpg" 1]
   [{:en "Metal (rough)"
     :de "Metall (rau)"} :metal-rough "/textures/metal-rough.jpg" 1]
   [{:en "Metal (scraped)"
     :de "Metall (zerkratzt)"} :metal-scraped "/textures/metal-scraped.jpg" 1]
   [{:en "Metal (smooth)"
     :de "Metall (glatt)"} :metal-smooth "/textures/metal-smooth.jpg" 1]
   [{:en "Mosaic"
     :de "Mosaik"} :mosaic "/textures/mosaic.jpg" 2]
   [{:en "Satin"
     :de "Satin"} :satin "/textures/satin.jpg" 10]
   [{:en "Stone"
     :de "Stein"} :stone "/textures/stone.jpg" 1]
   [{:en "Stone (rough)"
     :de "Stein (rau)"} :stone-rough "/textures/stone-rough.jpg" 1]
   [{:en "Wood"
     :de "Holz"} :wood "/textures/wood.jpg" 1]])

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
