(ns heraldicon.render.theme
  (:require
   [heraldicon.options :as options]))

(def theme-all
  {:all true})

;; colours taken from ;; https://github.com/drawshield/Drawshield-Code/blob/0d7ecc865e5ccd2ae8b17c11511c6afebb2ff6c9/svg/schemes/wappenwiki.txt
(def theme-wappenwiki
  {;; metals
   :argent "#f6f6f6"
   :argent-dark "#e5e5e5"
   :or "#f2bc51"
   ;; colours
   :azure "#0c6793"
   :vert "#3e933f"
   :gules "#bb2f2e"
   :sable "#333333"
   :purpure "#913a6a"
   ;; stains
   :murrey "#a32c45"
   :sanguine "#a42f2d"
   :tenne "#bf7433"
   ;; nontraditional
   :carnation "#e9bfa2"
   :brunatre "#725942"
   :cendree "#cbcaca"
   :rose "#d99292"
   :bleu-celeste "#359db7"
   :orange "#e58a39"
   :copper "#953d02"
   :buff "#ddc595"
   :white "#ffffff"})

(def theme-web
  {;; metals
   :argent "#ffffff"
   :or "#ffd700"
   ;; colours
   :azure "#0000ff"
   :vert "#008000"
   :gules "#ff0000"
   :sable "#000000"
   :purpure "#800080"
   ;; stains
   :murrey "#5f005f" ;; 30% darkened purpure
   :sanguine "#b20000" ;; 30% darkened gules
   :tenne "#c67000"})

;; Wikipedia themes: https://commons.wikimedia.org/wiki/Template:Tincture

(def theme-wikipedia-default
  {;; metals
   :argent "#ffffff"
   :argent-dark "#e7e7e7"
   :or "#fcdd09"
   ;; colours
   :azure "#0f47af"
   :vert "#078930"
   :gules "#da121a"
   :sable "#000000"
   :purpure "#9116a1"
   ;; stains
   :murrey "#650f70" ;; 30% darkened purpure
   :sanguine "#980c12" ;; 30% darkened gules
   :tenne "#9d5333"
   ;; nontraditional
   :bleu-celeste "#89c5e3"
   :carnation "#f2a772"
   :cendree "#999999"
   :orange "#eb7711"})

(def theme-wikipedia-web
  {;; metals
   :argent "#ffffff"
   :argent-dark "#eeeeee"
   :or "#ffdd11"
   ;; colours
   :azure "#1177cc"
   :vert "#119933"
   :gules "#dd2222"
   :sable "#000000"
   :purpure "#992288"
   ;; stains
   :murrey "#6b175f" ;; 30% darkened purpure
   :sanguine "#9a1717" ;; 30% darkened gules
   :tenne "#884411"
   ;; nontraditional
   :bleu-celeste "#33aaee"
   :carnation "#eebb99"
   :cendree "#779988"
   :orange "#ff6600"})

(def theme-wikipedia-bajuvarian
  {;; metals
   :argent "#ffffff"
   :argent-dark "#dddde6"
   :or "#ffd700"
   ;; colours
   :azure "#0033ff"
   :vert "#009900"
   :gules "#ff0000"
   :sable "#000000"
   :purpure "#880088"
   ;; stains
   :murrey "#5f005f" ;; 30% darkened purpure
   :sanguine "#b20000" ;; 30% darkened gules
   :tenne "#884411"
   ;; nontraditional
   :bleu-celeste "#0099ff"
   :carnation "#ffbb99"
   :cendree "#778899"
   :orange "#eb7711"})

(def theme-wikipedia-brandenburg
  {;; metals
   :argent "#ffffff"
   :or "#f5d306"
   ;; colours
   :azure "#0296C6"
   :vert "#0ca644"
   :gules "#e64625"
   :sable "#000000"
   ;; stains
   :sanguine "#a72c13" ;; 30% darkened gules
   :tenne "#884411"
   ;; nontraditional
   :bleu-celeste "#89c5e3"})

(def theme-wikipedia-wuerttemberg
  {;; metals
   :argent "#ffffff"
   :or "#fcdb00"
   ;; colours
   :azure "#005198"
   :vert "#0ca644"
   :gules "#da251d"
   :sable "#000000"
   ;; stains
   :sanguine "#981914" ;; 30% darkened gules
   :tenne "#884411"
   ;; nontraditional
   :bleu-celeste "#0081c9"
   :carnation "#f2b398"})

(def theme-wikipedia-switzerland
  {;; metals
   :argent "#ffffff"
   :or "#ffd72e"
   ;; colours
   :azure "#248bcc"
   :vert "#00a94d"
   :gules "#e7423f"
   :sable "#000000"
   ;; stains
   :sanguine "#b61916" ;; 30% darkened gules
   })

(def theme-wikipedia-spain
  {;; metals
   :argent "#ffffff"
   :argent-dark "#e7e7e7"
   :or "#eac102"
   ;; colours
   :azure "#0071bc"
   :vert "#008f4c"
   :gules "#ed1c24"
   :sable "#000000"
   :purpure "#630b57"
   ;; stains
   :murrey "#45073c" ;; 30% darkened purpure
   :sanguine "#ab0d13" ;; 30% darkened gules
   ;; nontraditional
   :carnation "#fcd3bc"
   :orange "#ff6600"})

(def theme-wikipedia-hungary
  {;; metals
   :argent "#ffffff"
   :argent-dark "#ececec"
   :or "#ffd200"
   ;; colours
   :azure "#0039a6"
   :vert "#009a3d"
   :gules "#dc281e"
   :sable "#000000"
   :purpure "#b60a9b"
   ;; stains
   :murrey "#7f066c" ;; 30% darkened purpure
   :sanguine "#9a1c15" ;; 30% darkened gules
   })

(def theme-wikipedia-sweden
  {;; metals
   :argent "#eeeeee"
   :argent-dark "#dddddd"
   :or "#ffcd50"
   ;; colours
   :azure "#003d8f"
   :vert "#225500"
   :gules "#d40000"
   :sable "#000000"
   :purpure "#aa235a"
   ;; stains
   :murrey "#76183f" ;; 30% darkened purpure
   :sanguine "#940000" ;; 30% darkened gules
   ;; nontraditional
   :bleu-celeste "#3291d7"
   :carnation "#eebb99"})

(def theme-ral-traffic
  {;; metals
   :argent "#f6f6f6" ;; RAL 9016
   :argent-dark "#929899" ;; RAL 7042
   :or "#f0ca00" ;; RAL 1023
   ;; colours
   :azure "#004c91" ;; RAL 5017
   :vert "#008351" ;; RAL 6024
   :gules "#bf111b" ;; RAL 3020
   :sable "#2a292a" ;; RAL 9017
   :purpure "#912d76" ;; RAL 4006
   ;; stains
   :murrey "#5f005f" ;; 30% darkened purpure
   :sanguine "#b20000" ;; 30% darkened gules
   :tenne "#884411"
   ;; nontraditional
   :bleu-celeste "#0099ff"
   :carnation "#ffbb99"
   :cendree "#4f5250" ;; RAL 7043
   :orange "#de5307" ;; RAL 2009
   })

;; https://fr.wikipedia.org/wiki/Projet:Blasons/Cr%C3%A9ation#Unification_des_couleurs

(def theme-wikipedia-france
  {;; metals
   :argent "#ffffff"
   :or "#fcef3c"
   ;; colours
   :azure "#2b5df2"
   :vert "#5ab532"
   :gules "#e20909"
   :sable "#000000"
   :purpure "#d576ad"
   ;; stains
   :murrey "#570b63"
   :sanguine "#a41619"
   :tenne "#9d5324"})

(def theme-community-pastell-puffs
  {;; metals
   :argent "#fefefe"
   :or "#ffe9c0"
   ;; colours
   :azure "#74badd"
   :vert "#85caad"
   :gules "#e87b97"
   :sable "#44457e"
   :purpure "#b284d5"
   ;; stains
   :murrey "#803db4" ;; 30% darkened purpure
   :sanguine "#d32451" ;; 30% darkened gules
   :tenne "#b27400" ;; 60% darkened or
   })

(def theme-community-jewelicious
  {;; metals
   :argent "#f8f7ec"
   :or "#fcdc90"
   ;; colours
   :azure "#2a67aa"
   :vert "#3e8f4c"
   :gules "#9a2829"
   :sable "#000000"
   :purpure "#884e91"
   ;; stains
   :murrey "#5f3665" ;; 30% darkened purpure
   :sanguine "#6b1b1c" ;; 30% darkened gules
   :tenne "#9a6d04" ;; 60% darkened or
   })

(def theme-community-main-seven
  {;; metals
   :argent "#fefefe"
   :or "#fcf7b2"
   ;; colours
   :azure "#4587d3"
   :vert "#5cbc63"
   :gules "#ec1642"
   :sable "#241f21"
   :purpure "#aa72be"
   ;; stains
   :murrey "#7c4391" ;; 30% darkened purpure
   :sanguine "#a60d2d" ;; 30% darkened gules
   :tenne "#a59a06" ;; 60% darkened or
   })

(def theme-community-cmwhyk
  {;; metals
   :argent "#fefefe"
   :or "#fff05a"
   ;; colours
   :azure "#5af0ff"
   :vert "#5aff5a"
   :gules "#fa5aff"
   :sable "#000000"
   :purpure "#5a60ff"
   ;; stains
   :murrey "#0008f1" ;; 30% darkened purpure
   :sanguine "#ea00f1" ;; 30% darkened gules
   :tenne "#8a7d00" ;; 60% darkened or
   })

(def theme-community-mother-earth
  {;; metals
   :argent "#fef8ed"
   :or "#f8d689"
   ;; colours
   :azure "#4e7b78"
   :vert "#7d9732"
   :gules "#b7431b"
   :sable "#3c1e15"
   :purpure "#a04463"
   ;; stains
   :murrey "#6f2f45" ;; 30% darkened purpure
   :sanguine "#802e12" ;; 30% darkened gules
   :tenne "#916708" ;; 60% darkened or
   })

(def theme-community-home-world
  {;; metals
   :argent "#fffff9"
   :or "#f5e73e"
   ;; colours
   :azure "#5c90f5"
   :vert "#64de57"
   :gules "#ea3951"
   :sable "#000000"
   :purpure "#c797e7"
   ;; stains
   :murrey "#9439d1" ;; 30% darkened purpure
   :sanguine "#b81329" ;; 30% darkened gules
   :tenne "#746c06" ;; 60% darkened or
   })

(def theme-community-crystal-gems
  {;; metals
   :argent "#fefefe"
   :or "#ffdd63"
   ;; colours
   :azure "#3e76e0"
   :vert "#3ebc6f"
   :gules "#fb1d7b"
   :sable "#000000"
   :purpure "#b083cd"
   ;; stains
   :murrey "#8043a7" ;; 30% darkened purpure
   :sanguine "#c00353" ;; 30% darkened gules
   :tenne "#8d6e00" ;; 60% darkened or
   })

(def theme-community-pretty-soldier
  {;; metals
   :argent "#fefefe"
   :or "#fff44b"
   ;; colours
   :azure "#0e89db"
   :vert "#31a449"
   :gules "#da0012"
   :sable "#000000"
   :purpure "#77468e"
   ;; stains
   :murrey "#533063" ;; 30% darkened purpure
   :sanguine "#98000c" ;; 30% darkened gules
   :tenne "#847b00" ;; 60% darkened or
   })

(def theme-community-the-monet-maker
  {;; metals
   :argent "#f2efec"
   :or "#f7d2a5"
   ;; colours
   :azure "#566e93"
   :vert "#a2a050"
   :gules "#a3403d"
   :sable "#1c2f36"
   :purpure "#a56b99"
   ;; stains
   :murrey "#76476c" ;; 30% darkened purpure
   :sanguine "#722c2a" ;; 30% darkened gules
   :tenne "#97590d" ;; 60% darkened or
   })

(def theme-community-van-goes-vroem
  {;; metals
   :argent "#eae8e3"
   :or "#f7d2a5"
   ;; colours
   :azure "#4c7cad"
   :vert "#88934d"
   :gules "#ca6548"
   :sable "#191b21"
   :purpure "#955f81"
   ;; stains
   :murrey "#68425a" ;; 30% darkened purpure
   :sanguine "#94422b" ;; 30% darkened gules
   :tenne "#97590d" ;; 60% darkened or
   })

(def theme-community-cotton-candy
  {;; metals
   :argent "#ffffff"
   :or "#fcf9d1"
   ;; colours
   :azure "#75def3"
   :vert "#80ebba"
   :gules "#f78fa9"
   :sable "#4b6667"
   :purpure "#dba5f4"
   ;; stains
   :murrey "#af37e6" ;; 30% darkened purpure
   :sanguine "#ef2155" ;; 30% darkened gules
   :tenne "#ada10b" ;; 60% darkened or
   })

(def theme-community-rainbow-groom
  {;; metals
   :argent "#f8f4db"
   :or "#ffd71b"
   ;; colours
   :azure "#6179cf"
   :vert "#8fb63f"
   :gules "#ea3b0c"
   :sable "#342818"
   :purpure "#aa7aba"
   ;; stains
   :murrey "#7c498d" ;; 30% darkened purpure
   :sanguine "#a32908" ;; 30% darkened gules
   :tenne "#705d00" ;; 60% darkened or
   })

(def theme-community-content-cranium
  {;; metals
   :argent "#ffffff"
   :or "#fffa00"
   ;; colours
   :azure "#0073b1"
   :vert "#93ad00"
   :gules "#fc0000"
   :sable "#1c1616"
   :purpure "#cd00ed"
   ;; stains
   :murrey "#8f00a5" ;; 30% darkened purpure
   :sanguine "#b00000" ;; 30% darkened gules
   :tenne "#666400" ;; 60% darkened or
   })

(def theme-community-sodacan
  {;; metals
   :argent "#ececec"
   :or "#e7cd54"
   ;; colours
   :azure "#1353b4"
   :vert "#4c8a1e"
   :gules "#ce0f25"
   :sable "#2d2d2d"
   :purpure "#a44476"
   ;; stains
   :murrey "#9a234e"
   :sanguine "#791717"
   :tenne "#c15f1d"
   ;; other
   :bleu-celeste "#66c8e1"
   :brunatre "#693310"
   :buff "#dfad42"
   :carnation "#fcbf8c"
   :cendree "#999999"
   :orange "#e35d03"
   :rose "#df2ddf"})

(def themes
  [[:string.theme.group/general
    [:string.theme/wappenwiki :wappenwiki theme-wappenwiki]
    [:string.theme/web :web theme-web]
    [:string.theme/ral-traffic :ral-traffic theme-ral-traffic]
    [:string.theme/all :all theme-all]]
   ["Wikipedia"
    [:string.theme/wikipedia-default :wikipedia-default theme-wikipedia-default]
    [:string.theme/wikipedia-web :wikipedia-web theme-wikipedia-web]
    [:string.theme/wikipedia-bajuvarian :wikipedia-bajuvarian theme-wikipedia-bajuvarian]
    [:string.theme/wikipedia-brandenburg :wikipedia-brandenburg theme-wikipedia-brandenburg]
    [:string.theme/wikipedia-wurttemberg :wikipedia-wurttemberg theme-wikipedia-wuerttemberg]
    [:string.theme/wikipedia-france :wikipedia-france theme-wikipedia-france]
    [:string.theme/wikipedia-hungary :wikipedia-hungary theme-wikipedia-hungary]
    [:string.theme/wikipedia-spain :wikipedia-spain theme-wikipedia-spain]
    [:string.theme/wikipedia-sweden :wikipedia-sweden theme-wikipedia-sweden]
    [:string.theme/wikipedia-switzerland :wikipedia-switzerland theme-wikipedia-switzerland]]
   [:string.theme.group/community
    ["CMwhyK" :community-cmwhyk theme-community-cmwhyk]
    ["Cotton Candy" :community-cotton-candy theme-community-cotton-candy]
    ["Crystal Gems" :community-crystal-gems theme-community-crystal-gems]
    ["Home World" :community-home-world theme-community-home-world]
    ["Jewelicious" :community-jewelicious theme-community-jewelicious]
    ["Main Seven" :community-main-seven theme-community-main-seven]
    ["Mother Earth" :community-mother-earth theme-community-mother-earth]
    ["Pastell Puffs" :community-pastell-puffs theme-community-pastell-puffs]
    ["Pretty Soldier" :community-pretty-soldier theme-community-pretty-soldier]
    ["Rainbow Groom" :community-rainbow-groom theme-community-rainbow-groom]
    ["The Monet Maker" :community-the-monet-maker theme-community-the-monet-maker]
    ["Van Goes Vroem" :community-van-goes-vroem theme-community-van-goes-vroem]
    ["Content Cranium" :community-content-cranium theme-community-content-cranium]
    ["Sodacan by Bananasplit1611" :community-sodacan theme-community-sodacan]]])

(def default
  :wappenwiki)

(def theme-choices
  (->> themes
       (map (fn [[group-name & items]]
              (vec (concat [group-name] (->> items
                                             (map (fn [[display-name key _]]
                                                    [display-name key])))))))
       vec))

(def theme-map
  (options/choices->map theme-choices))

(def theme-data-map
  (->> themes
       (map (fn [[_group-name & items]]
              (->> items
                   (map (fn [[_ key colours]]
                          [key colours])))))
       (apply concat)
       (into {})))

(defn lookup-colour [tincture theme]
  (let [theme-colours (merge
                       (get theme-data-map default)
                       (get theme-data-map theme))]
    (get theme-colours tincture)))
