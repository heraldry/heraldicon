(ns heraldicon.render.theme
  (:require
   [heraldicon.options :as options]))

(def ^:private theme-all
  {:all true

   ::name :string.theme/all
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Heraldicon"
                  :creator-link "https://heraldicon.org"}})

;; colours taken from ;; https://github.com/drawshield/Drawshield-Code/blob/0d7ecc865e5ccd2ae8b17c11511c6afebb2ff6c9/svg/schemes/wappenwiki.txt
(def ^:private theme-wappenwiki
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
   :white "#ffffff"
   :amaranth "#b51973"
   :teal "#00b5b8"
   ;; special
   :helmet-light "#d8d8d8"
   :helmet-medium "#989898"
   :helmet-dark "#585858"

   ::name :string.theme/wappenwiki
   ::attribution {:nature :own-work
                  :license :cc-attribution-non-commercial-share-alike
                  :license-version :v3
                  :creator-name "WappenWiki"
                  :creator-link "https://wappenwiki.org"}})

(def ^:private theme-encyclopedia-heraldica
  {;; metals
   :argent "#ffffff"
   :or "#fcc900"
   ;; colours
   :azure "#003d8f"
   :vert "#037536"
   :gules "#d40000"
   :sable "#332e2d"
   :purpure "#4e2f7a"
   ;; stains
   :murrey "#630727"
   :sanguine "#800000"
   :tenne "#bf7532"
   ;; nontraditional
   :carnation "#ebbb9a"
   :cendree "#999999"
   :rose "#e63d6a"
   :bleu-celeste "#3581b9"
   :orange "#ed5f00"
   :copper "#953d02"
   :buff "#ddc595"

   ::name "Encyclopedia Heraldica"
   ::attribution {:nature :own-work
                  :license :cc-attribution
                  :license-version :v4
                  :creator-name "Encyclopedia Heraldica"
                  :creator-link "https://1drv.ms/u/s!Anj4BrtS8clIaQi3EIOCPpnfKQE?e=AkQ8lW"}})

(def ^:private wiki-attribution
  {:nature :own-work
   :license :cc-attribution-share-alike
   :license-version :v3
   :creator-name "Wikipedia"
   :creator-link "https://wikipedia.org"})

(def ^:private theme-web
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
   :tenne "#c67000"

   ::name :string.theme/web
   ::attribution wiki-attribution})

;; Wikipedia themes: https://commons.wikimedia.org/wiki/Template:Tincture

(def ^:private theme-wikipedia-default
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
   :orange "#eb7711"

   ::name :string.theme/wikipedia-default
   ::attribution wiki-attribution})

(def ^:private theme-wikipedia-web
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
   :orange "#ff6600"

   ::name :string.theme/wikipedia-web
   ::attribution wiki-attribution})

(def ^:private theme-wikipedia-bajuvarian
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
   :orange "#eb7711"

   ::name :string.theme/wikipedia-bajuvarian
   ::attribution wiki-attribution})

(def ^:private theme-wikipedia-brandenburg
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
   :bleu-celeste "#89c5e3"

   ::name :string.theme/wikipedia-brandenburg
   ::attribution wiki-attribution})

(def ^:private theme-wikipedia-wuerttemberg
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
   :carnation "#f2b398"

   ::name :string.theme/wikipedia-wurttemberg
   ::attribution wiki-attribution})

(def ^:private theme-wikipedia-switzerland
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

   ::name :string.theme/wikipedia-switzerland
   ::attribution wiki-attribution})

(def ^:private theme-wikipedia-spain
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
   :orange "#ff6600"

   ::name :string.theme/wikipedia-spain
   ::attribution wiki-attribution})

(def ^:private theme-wikipedia-hungary
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

   ::name :string.theme/wikipedia-hungary
   ::attribution wiki-attribution})

(def ^:private theme-wikipedia-sweden
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
   :carnation "#eebb99"

   ::name :string.theme/wikipedia-sweden
   ::attribution wiki-attribution})

(def ^:private theme-ral-traffic
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

   ::name :string.theme/ral-traffic
   ::attribution wiki-attribution})

;; https://fr.wikipedia.org/wiki/Projet:Blasons/Cr%C3%A9ation#Unification_des_couleurs

(def ^:private theme-wikipedia-france
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
   :tenne "#9d5324"

   ::name :string.theme/wikipedia-france
   ::attribution wiki-attribution})

(def ^:private taritoons-attribution
  {:nature :own-work
   :license :public-domain
   :link "https://www.reddit.com/r/heraldry/comments/i8dy38/some_heraldic_tincture_palettes_for_you_to_use/"
   :creator-name "TariToons"
   :creator-link "https://www.taritoons.de/"})

(def ^:private theme-community-pastell-puffs
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

   ::name "Pastell Puffs"
   ::attribution taritoons-attribution})

(def ^:private theme-community-jewelicious
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

   ::name "Jewelicious"
   ::attribution taritoons-attribution})

(def ^:private theme-community-main-seven
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

   ::name "Main Seven"
   ::attribution taritoons-attribution})

(def ^:private theme-community-cmwhyk
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

   ::name "CMwhyK"
   ::attribution taritoons-attribution})

(def ^:private theme-community-mother-earth
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

   ::name "Mother Earth"
   ::attribution taritoons-attribution})

(def ^:private theme-community-home-world
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

   ::name "Home World"
   ::attribution taritoons-attribution})

(def ^:private theme-community-crystal-gems
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

   ::name "Crystal Gems"
   ::attribution taritoons-attribution})

(def ^:private theme-community-pretty-soldier
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

   ::name "Pretty Soldier"
   ::attribution taritoons-attribution})

(def ^:private theme-community-the-monet-maker
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

   ::name "The Monet Maker"
   ::attribution taritoons-attribution})

(def ^:private theme-community-van-goes-vroem
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

   ::name "Van Goes Vroom"
   ::attribution taritoons-attribution})

(def ^:private theme-community-cotton-candy
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

   ::name "Cotton Candy"
   ::attribution taritoons-attribution})

(def ^:private theme-community-rainbow-groom
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

   ::name "Rainbow Groom"
   ::attribution taritoons-attribution})

(def ^:private theme-community-main-72
  {;; metals
   :argent "#ffffff"
   :or "#fcf7b2"
   ;; colours
   :azure "#367ec1"
   :vert "#60b866"
   :gules "#dc2764"
   :sable "#513951"
   :purpure "#aa63c4"
   ;; stains
   :murrey "#7d3996" ;; 30% darkened purpure
   :sanguine "#9c1945" ;; 30% darkened gules
   :tenne "#a69b06" ;; 60% darkened or

   ::name "Main 72"
   ::attribution {:license :public-domain
                  :creator-name "TariToons"
                  :creator-link "https://www.taritoons.de/"}})

(def ^:private theme-community-content-cranium
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

   ::name "Content Cranium"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Happy Skull#9393"}})

(def ^:private theme-community-sodacan
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
   :rose "#df2ddf"

   ::name "Sodacan"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Bananasplit1611"}})

(def ^:private theme-community-custom-philippine
  {;; metals
   :argent "#ffffff"
   :or "#fcd116"
   ;; colours
   :azure "#0038a8"
   :vert "#177245"
   :gules "#ce1126"
   :sable "#161616"
   :purpure "#99004d"
   ;; stains
   :murrey "#6b0035" ;; 30% darkened purpure
   :sanguine "#900b1a" ;; 30% darkened gules
   :tenne "#6c5801" ;; 60% darkened or

   ::name "Custom Philippine"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "coinageFission"}})

(def ^:private theme-community-soft-rage
  {;; metals
   :argent "#eeeeee"
   :or "#eac05d"
   ;; colours
   :azure "#2c84cc"
   :vert "#36af63"
   :gules "#e02a36"
   :sable "#3f3f3f"
   :purpure "#a23bcc"
   ;; stains
   :murrey "#dd2a6c"
   :sanguine "#c63943"
   :tenne "#e88c3c"
   ;; other
   :amaranth "#dd2a6c"
   :bleu-celeste "#5ab7ce"
   :brunatre "#c67533"
   :buff "#ffe9d6"
   :carnation "#ffe4d6"
   :cendree "#707070"
   :copper "#e5cdc0"
   :orange "#e88c3c"
   :rose "#dd7cb1"
   :white "#ffffff"
   ;; special
   :helmet-light "#cccccc"
   :helmet-medium "#707070"
   :helmet-dark "#515151"

   ::name "Dughorm's Palette"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Dughorm"}})

(def ^:private theme-community-the-violet-herald-palette
  {;; metals
   :argent "#bcc6cc"
   :or "#ffc800"
   ;; colours
   :azure "#0000b0"
   :vert "#046a38"
   :gules "#e10000"
   :sable "#000000"
   :purpure "#670099"
   ;; stains
   :murrey "#800040"
   :sanguine "#b40000"
   :tenne "#953d02"
   ;; other
   :brunatre "#411c06"
   :buff "#ddc595"
   :carnation "#e6b488"
   :copper "#bf7532"
   :orange "#ff5500"
   :rose "#e63d6a"
   :white "#ffffff"

   ::name "The Violet Herald"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Violet Herald"}})

(def ^:private theme-community-tinctures2go
  {;; metals
   :argent "#eeeeee"
   :or "#ccab66"
   ;; colours
   :azure "#3d658f"
   :vert "#3e8f4c"
   :gules "#8f3d3e"
   :sable "#333333"
   :purpure "#833d8f"
   ;; stains
   :murrey "#6b2e44"
   :sanguine "#6b2e2e"
   :tenne "#997533"
   ;; other
   :amaranth "#8f3d74"
   :brunatre "#6b4c2e"
   :buff "#d1bd94"
   :carnation "#c9ae9c"
   :copper "#cc9c66ff"
   :orange "#bf8540"
   :rose "#c27070"
   :white "#fcfcfc"
   :cendree "#999999"
   :bleu-celeste "#70bbc2"

   ::name "Tinctures2Go"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private theme-community-commodore64
  {;; metals
   :argent "#adadad"
   :or "#c9d487" ;; gold
   ;; colours
   :azure "#50459b"
   :vert "#5cab5e"
   :gules "#cb7e75"
   :sable "#000000"
   :purpure "#a057a3"
   ;; stains
   :murrey "#703d72" ;; 30% darkened purpure
   :sanguine "#9f4e44"
   :tenne "#a1683c"
   ;; other
   :amaranth "#887ecb"
   :brunatre "#6d5412"
   :white "#ffffff"
   :cendree "#626262"
   :bleu-celeste "#6abfc6"

   ::name "Commodore64"
   ::attribution {:nature :derivative
                  :license :public-domain
                  :creator-name "vairy"
                  :creator-link "https://heraldicon.org/users/vairy"
                  :source-license :public-domain
                  :source-name "COMMODORE 64 PALETTE"
                  :source-link "https://lospec.com/palette-list/commodore64"
                  :source-creator-name "Lospec"
                  :source-creator-link "https://lospec.com"}})

(def ^:private theme-community-mutcd
  {;; metals
   :argent "#cdcdcd" ;;
   :or "#ffcd00" ;; 116C

   ;; colours
   :azure "#003f87" ;; 294C
   :vert "#006747" ;; 342C
   :gules "#bf2033" ;; 187C
   :sable "#000000" ;;
   :purpure "#6d2077" ;; 259C

   ;; other
   :amaranth "#df4661" ;;
   :brunatre "#693f23" ;; 469C
   :buff "#ddcba4" ;; 468C
   :copper "#c4d600" ;;
   :orange "#f38f00" ;;

   ::name "MUTCD"
   ::attribution {:license :public-domain
                  :creator-name "vairy"
                  :creator-link "https://heraldicon.org/users/vairy"
                  :source-license :public-domain
                  :source-name "MUTCD colors"
                  :source-link "https://commons.wikimedia.org/wiki/commons:WikiProject_U.S._Roads/Shields#MUTCD_colors"
                  :source-creator-name "FHWA"
                  :source-creator-link "https://highways.dot.gov/"}})

(def ^:private theme-community-minecraft
  {;; metal
   :argent "#eaeded"
   :or "#f9c629"

   ;; color
   :azure "#353a9e"
   :vert "#556e1c"
   :gules "#a12823"
   :sable "#16161b"
   :purpure "#7b2bad"

   ;; stain
   :murrey "#972947"
   :sanguine "#720000"
   :tenne "#a25426"

   ;; other
   :amaranth "#a9309f"
   :brunatre "#734829"
   :buff "#4d3324"
   :carnation "#d2b2a1"
   :copper "#e77c56"
   :orange "#f17716"
   :rose "#ee90ad"
   :white "#ffffff"
   :cendree "#8e8f87"
   :bleu-celeste "#3cb0da"

   ::name "Minecraft"
   ::attribution {:license :public-domain
                  :creator-name "abf3427"
                  :creator-link "https://twitter.com/abf3427"}})

(def ^:private theme-community-ahtinctures
  {;; metals
   :argent "#fffefe" ;; silver
   :or "#ffe66e" ;; gold

   ;; colours
   :azure "#0350b6"
   :vert "#53872c"
   :gules "#ce0001"
   :sable "#323233"
   :purpure "#a84078"

   ;; stains
   :murrey "#670000"
   :sanguine "#900000"
   :tenne "#ce7e19"

   ;; other
   :amaranth "#b51872"
   :brunatre "#745a49"
   :buff "#ebb24c"
   :carnation "#fab587"
   :copper "#AB6334"
   :orange "#E48634"
   :rose "#D99392"
   :white "#FFFFFF"
   :cendree "#f0f1ea"
   :bleu-celeste "#9bd2fe"

   ::name "aHTinctures"
   ::attribution {:license :public-domain
                  :creator-name "ashoppio"
                  :creator-link "https://heraldicon.org/users/ashoppio"}})

(def ^:private themes
  [[:string.theme.group/general
    [:wappenwiki theme-wappenwiki]
    [:encyclopedia-heraldica theme-encyclopedia-heraldica]
    [:web theme-web]
    [:ral-traffic theme-ral-traffic]
    [:all theme-all]]
   ["Wikipedia"
    [:wikipedia-default theme-wikipedia-default]
    [:wikipedia-web theme-wikipedia-web]
    [:wikipedia-bajuvarian theme-wikipedia-bajuvarian]
    [:wikipedia-brandenburg theme-wikipedia-brandenburg]
    [:wikipedia-wurttemberg theme-wikipedia-wuerttemberg]
    [:wikipedia-france theme-wikipedia-france]
    [:wikipedia-hungary theme-wikipedia-hungary]
    [:wikipedia-spain theme-wikipedia-spain]
    [:wikipedia-sweden theme-wikipedia-sweden]
    [:wikipedia-switzerland theme-wikipedia-switzerland]]
   [:string.theme.group/community
    [:community-cmwhyk theme-community-cmwhyk]
    [:community-cotton-candy theme-community-cotton-candy]
    [:community-crystal-gems theme-community-crystal-gems]
    [:community-home-world theme-community-home-world]
    [:community-jewelicious theme-community-jewelicious]
    [:community-main-seven theme-community-main-seven]
    [:community-main-72 theme-community-main-72]
    [:community-mother-earth theme-community-mother-earth]
    [:community-pastell-puffs theme-community-pastell-puffs]
    [:community-pretty-soldier theme-community-pretty-soldier]
    [:community-rainbow-groom theme-community-rainbow-groom]
    [:community-the-monet-maker theme-community-the-monet-maker]
    [:community-van-goes-vroem theme-community-van-goes-vroem]
    [:community-content-cranium theme-community-content-cranium]
    [:community-sodacan theme-community-sodacan]
    [:community-custom-philippine theme-community-custom-philippine]
    [:community-soft-rage theme-community-soft-rage]
    [:community-the-violet-herald-palette theme-community-the-violet-herald-palette]
    [:community-tinctures2go theme-community-tinctures2go]
    [:community-commodore64 theme-community-commodore64]
    [:community-minecraft theme-community-minecraft]
    [:community-ahtinctures theme-community-ahtinctures]
    [:community-mutcd theme-community-mutcd]]])

(def default
  :wappenwiki)

(def ^:private kinds-map
  (into {}
        (for [[_ & items] themes
              [key value] items]
          [key value])))

(def choices
  (mapv (fn [[group-name & items]]
          (into [group-name]
                (map (fn [[key data]]
                       [(::name data) key]))
                items))
        themes))

(def theme-map
  (options/choices->map choices))

(def theme-data-map
  (into {}
        (mapcat (fn [[_group-name & items]]
                  (map (fn [[key data]]
                         [key data])
                       items)))
        themes))

(defn lookup-colour [tincture theme]
  (get-in theme-data-map [theme tincture]
          (get-in theme-data-map [default tincture])))

(defn attribution [theme]
  (let [{::keys [attribution name]} (get kinds-map theme)]
    (assoc attribution :name name)))
