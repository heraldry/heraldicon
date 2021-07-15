(ns heraldry.font
  (:require [heraldry.util :as util]))

(def default :prince-valiant)

(def choices
  [["Black Chancery" :black-chancery]
   ["Cardinal" :cardinal]
   ["Carolinga" :carolinga]
   ["Lohengrin" :lohengrin]
   ["Magic School" :magic-school]
   ["Prince Valiant" :prince-valiant]])

(def choice-map
  (util/choices->map choices))

(defn css-string [font]
  (get choice-map (or font default)))

(def default-options
  {:type :choice
   :choices choices
   :default default
   :ui {:label "Font"}})
