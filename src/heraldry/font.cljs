(ns heraldry.font
  (:require [heraldry.util :as util]))

(def default :prince-valiant)

(def choices
  [["Baskerville Berthold" :baskerville-berthold]
   ["Black Chancery" :black-chancery]
   ["Cardinal" :cardinal]
   ["Carolinga" :carolinga]
   ["Cloister" :cloister]
   ["DejaVuSans" :deja-vu-sans]
   ["DejaVuSerif" :deja-vu-serif]
   ["Garamond" :garamond]
   ["KJV1611" :kjv1611]
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

