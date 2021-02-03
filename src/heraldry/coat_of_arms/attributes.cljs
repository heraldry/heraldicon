(ns heraldry.coat-of-arms.attributes)

(defn options->map [options]
  (->> options
       (map (fn [[group-name & items]]
              (if (and (-> items count (= 1))
                       (-> items first keyword?))
                ;; in this case there is no group, treat the first element of "items" as key
                ;; and "group-name" as display-name
                [[(first items) group-name]]
                (->> items
                     (map (comp vec reverse))))))
       (apply concat)
       (into {})))

(def attitude-options
  [["None" :none]
   ["Beasts"
    ["Couchant" :couchant]
    ["Courant" :courant]
    ["Dormant" :dormant]
    ["Pascuant" :pascuant]
    ["Passant" :passant]
    ["Rampant" :rampant]
    ["Salient" :salient]
    ["Sejant" :sejant]
    ["Statant" :statant]
    ["Segreant" :segreant]]
   ["Two beasts"
    ["Combatant" :combatant]
    ["Respectant" :respectant]
    ["Addorsed" :addorsed]]
   ["Winged creatures"
    ["Displayed" :displayed]
    ["Overt" :overt]
    ["Close" :close]
    ["Issuant" :issuant]
    ["Rising" :rising]
    ["Volant" :volant]
    ["Recursant" :recursant]
    ["Vigilant" :vigilant]
    ["Vulning herself" :vulning]
    ["In her piety" :in-her-piety]]
   ["Sea creatures"
    ["Naiant" :naiant]
    ["Urinant" :urinant]
    ["Hauriant" :hauriant]]
   ["Serpents"
    ["Nowed" :nowed]
    ["Glissant" :glissant]]])

(def attitude-map
  (options->map attitude-options))

(def facing-options
  [["None" :none]
   ["To dexter" :to-dexter]
   ["To sinister" :to-sinister]
   ["Affronté" :affronte]
   ["En arrière" :en-arriere]
   ["Guardant" :guardant]
   ["Reguardant" :reguardant]
   ["Salient" :salient]
   ["In trian aspect" :in-trian-aspect]])

(def facing-map
  (options->map facing-options))
