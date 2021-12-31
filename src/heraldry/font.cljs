(ns heraldry.font
  (:require
   [heraldry.util :as util]))

(def default :prince-valiant)

(def font-data
  [["Baskerville Berthold" :baskerville-berthold "/baskerville/BaskervilleBE-Regular.otf"]
   ["Black Chancery" :black-chancery "/black-chancery/BLKCHCRY.TTF"]
   ["Cardinal" :cardinal "/cardinal/Cardinal.ttf"]
   ["Carolinga" :carolinga "/carolinga/CAROBTN_.TTF"]
   ["Cloister" :cloister "/cloister/CloisterBlack.ttf"]
   ["DejaVuSans" :deja-vu-sans "/deja-vu/DejaVuSans.ttf"]
   ["DejaVuSerif" :deja-vu-serif "/deja-vu/DejaVuSerif.ttf"]
   ["Garamond" :garamond "/garamond/Garamond Regular.ttf"]
   ["KJV1611" :kjv1611 "/kjv1611/KJV1611.otf"]
   ["Liturgisch" :liturgisch "/liturgisch/liturgisch.regular.ttf"]
   ["Lohengrin" :lohengrin "/lohengrin/Lohengrin.ttf"]
   ["Magic School" :magic-school "/magic-school/MagicSchoolOne.ttf"]
   ["Prince Valiant" :prince-valiant "/prince-valiant/PrinceValiant.ttf"]])

(def choices
  (->> font-data
       (mapcat (fn [[font-name key _path]]
                 [[font-name key]]))
       vec))

(def choice-map
  (util/choices->map choices))

(def path-map
  (->> font-data
       (mapcat (fn [[_font-name key path]]
                 [[key path]]))
       (into {})))

(defn css-string [font]
  (get choice-map (or font default)))

(def default-options
  {:type :choice
   :choices choices
   :default default
   :ui {:label :string.option/font}})
