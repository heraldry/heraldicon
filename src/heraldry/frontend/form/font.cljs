(ns heraldry.frontend.form.font
  (:require [heraldry.frontend.form.element :as element]
            [heraldry.util :as util]))

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

(defn form [path]
  [element/select path "Font" choices
   :default default])
