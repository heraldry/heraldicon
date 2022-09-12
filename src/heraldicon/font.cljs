(ns heraldicon.font
  (:require
   [heraldicon.options :as options]))

(def default :prince-valiant)

(def font-data
  [["Berthold Baskerville" :baskerville-berthold "/baskerville/BaskervilleBE-Regular.otf"]
   ["BlackChancery" :black-chancery "/black-chancery/BLKCHCRY.TTF"]
   ["Cardinal" :cardinal "/cardinal/Cardinal.ttf"]
   ["Carolingia" :carolinga "/carolinga/CAROBTN_.TTF"]
   ["Cloister Black" :cloister "/cloister/CloisterBlack.ttf"]
   ["DejaVu Sans" :deja-vu-sans "/deja-vu/DejaVuSans.ttf"]
   ["DejaVu Serif" :deja-vu-serif "/deja-vu/DejaVuSerif.ttf"]
   ["Fondamento" :fondamento "/fondamento/Fondamento-Regular.ttf"]
   ["Garamond" :garamond "/garamond/Garamond Regular.ttf"]
   ["KJV1611" :kjv1611 "/kjv1611/KJV1611.otf"]
   ["Uncial Antiqua" :uncial-antiqua "/uncial-antiqua/UncialAntiqua-Regular.ttf"]
   ["Liturgisch" :liturgisch "/liturgisch/liturgisch.regular.ttf"]
   ["Lohengrin" :lohengrin "/lohengrin/Lohengrin.ttf"]
   ["Magic School One" :magic-school "/magic-school/MagicSchoolOne.ttf"]
   ["Prince Valiant" :prince-valiant "/prince-valiant/PrinceValiant.ttf"]
   ["Trajan Pro" :trajan-pro "/trajan-pro/Trajan Pro Regular.ttf"]])

(def choices
  (map (fn [[font-name key _path]]
         [font-name key])
       font-data))

(def font-map
  (options/choices->map choices))

(def path-map
  (into {}
        (map (fn [[_font-name key path]]
               [key path]))
        font-data))

(defn css-string [font]
  (get font-map (or font default)))

(def default-options
  {:type :option.type/choice
   :choices choices
   :default default
   :ui/label :string.option/font})
