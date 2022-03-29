(ns heraldry.blazonry.test-parser
  (:require
   [cljs.test :refer-macros [are deftest]]
   [heraldry.blazonry.parser :as blazonry-parser]))

(deftest transforming
  (are [ast form] (= (blazonry-parser/ast->hdn ast) form)

    [:A "a"] 1
    [:A "an"] 1

    [:NUMBER "123"] 123
    [:NUMBER "0134"] 134

    [:number-word [:DIGIT-WORD "one"]] 1
    [:number-word "twelve"] 12
    [:number-word [:MULTILPE-OF-TEN-WORD "eighty"]] 80
    [:number-word [:MULTILPE-OF-TEN-WORD "eighty"] " " [:DIGIT-WORD "one"]] 81
    [:number-word [:MULTILPE-OF-TEN-WORD "eighty"] "-" [:DIGIT-WORD "one"]] 81
    [:number-word [:MULTILPE-OF-TEN-WORD "eighty"] "" [:DIGIT-WORD "one"]] 81
    [:number-word [:MULTILPE-OF-TEN-WORD "twenty"] "" [:DIGIT-WORD "seven"]] 27

    [:number-word [:MULTI-WORD "double"]] 2
    [:number-word [:MULTI-WORD "triple"]] 3))

(deftest parsing
  (are [blazon form] (= (blazonry-parser/blazon->hdn blazon) form)

    "or"
    {:type :heraldry.field.type/plain
     :tincture :or}

    "tenné"
    {:type :heraldry.field.type/plain
     :tincture :tenne}

    "erminois"
    {:type :heraldry.field.type/plain
     :tincture :erminois}

    "or a fess gules, pale sable"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}}
                  {:type :heraldry.ordinary.type/pale
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}]}

    "or a pale sable cottised wavy azure and gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/pale
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}
                   :cottising {:cottise-1 {:field {:type :heraldry.field.type/plain
                                                   :tincture :azure}
                                           :line {:type :wavy}}
                               :cottise-2 {:field {:type :heraldry.field.type/plain
                                                   :tincture :gules}}
                               :cottise-opposite-1 {:field {:type :heraldry.field.type/plain
                                                            :tincture :azure}
                                                    :line {:type :wavy}}
                               :cottise-opposite-2 {:field {:type :heraldry.field.type/plain
                                                            :tincture :gules}}}}]}

    "or a pall sable cottised wavy azure and gules, cottices straight and indented argent, with doubly cottising urdy vert"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/pall
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}
                   :cottising {:cottise-1 {:field {:type :heraldry.field.type/plain
                                                   :tincture :azure}
                                           :line {:type :wavy}}
                               :cottise-2 {:field {:type :heraldry.field.type/plain
                                                   :tincture :gules}}
                               :cottise-opposite-1 {:field {:type :heraldry.field.type/plain
                                                            :tincture :argent}
                                                    :line {:type :straight}
                                                    :opposite-line {:type :indented}}

                               :cottise-extra-1 {:field {:type :heraldry.field.type/plain
                                                         :tincture :vert}
                                                 :line {:type :urdy}}
                               :cottise-extra-2 {:field {:type :heraldry.field.type/plain
                                                         :tincture :vert}
                                                 :line {:type :urdy}}}}]}

    "or five tressures sable"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/orle
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}
                  {:type :heraldry.ordinary.type/orle
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}
                  {:type :heraldry.ordinary.type/orle
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}
                  {:type :heraldry.ordinary.type/orle
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}
                  {:type :heraldry.ordinary.type/orle
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}]}

    "or a fess enhanced gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :origin {:offset-y 12.5}
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}}]}

    "or a fess dehanced gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :origin {:offset-y -12.5}
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}}]}

    "or a chevron reversed gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/chevron
                   :direction-anchor {:point :chief}
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}}]}

    "or a pile reversed gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/pile
                   :origin {:point :bottom}
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}}]}

    "or a fess indented or, pale straight and urdy fimbriated or and argent sable"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :line {:type :indented}
                   :field {:type :heraldry.field.type/plain
                           :tincture :or}}
                  {:type :heraldry.ordinary.type/pale
                   :line {:type :straight}
                   :opposite-line {:type :urdy
                                   :fimbriation {:mode :double
                                                 :tincture-1 :or
                                                 :tincture-2 :argent}}
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}]}

    "per fess sable and ermine"
    {:type :heraldry.field.type/per-fess
     :fields [{:type :heraldry.field.type/plain
               :tincture :sable}
              {:type :heraldry.field.type/plain
               :tincture :ermine}]}

    "per fess sable and ermine"
    {:type :heraldry.field.type/per-fess
     :fields [{:type :heraldry.field.type/plain
               :tincture :sable}
              {:type :heraldry.field.type/plain
               :tincture :ermine}]}

    "per fess dancetty sable and ermine"
    {:type :heraldry.field.type/per-fess
     :line {:type :dancetty}
     :fields [{:type :heraldry.field.type/plain
               :tincture :sable}
              {:type :heraldry.field.type/plain
               :tincture :ermine}]}

    "per fess fimbriated or sable and ermine"
    {:type :heraldry.field.type/per-fess
     :line {:fimbriation {:mode :single
                          :tincture-1 :or}}
     :fields [{:type :heraldry.field.type/plain
               :tincture :sable}
              {:type :heraldry.field.type/plain
               :tincture :ermine}]}

    "per fess fimbriated or and vert sable and ermine"
    {:type :heraldry.field.type/per-fess
     :line {:fimbriation {:mode :double
                          :tincture-1 :or
                          :tincture-2 :vert}}
     :fields [{:type :heraldry.field.type/plain
               :tincture :sable}
              {:type :heraldry.field.type/plain
               :tincture :ermine}]}

    "tierced per pale sable and ermine and azure"
    {:type :heraldry.field.type/tierced-per-pale
     :fields [{:type :heraldry.field.type/plain
               :tincture :sable}
              {:type :heraldry.field.type/plain
               :tincture :ermine}
              {:type :heraldry.field.type/plain
               :tincture :azure}]}

    "tierced per pale indented sable and ermine and azure"
    {:type :heraldry.field.type/tierced-per-pale
     :line {:type :indented}
     :fields [{:type :heraldry.field.type/plain
               :tincture :sable}
              {:type :heraldry.field.type/plain
               :tincture :ermine}
              {:type :heraldry.field.type/plain
               :tincture :azure}]}

    "tierced per pale indented and dovetailed fimbriated azure and gules sable and ermine and azure"
    {:type :heraldry.field.type/tierced-per-pale
     :line {:type :indented}
     :opposite-line {:type :dovetailed
                     :fimbriation {:mode :double
                                   :tincture-1 :azure
                                   :tincture-2 :gules}}
     :fields [{:type :heraldry.field.type/plain
               :tincture :sable}
              {:type :heraldry.field.type/plain
               :tincture :ermine}
              {:type :heraldry.field.type/plain
               :tincture :azure}]}

    "or a fess humetty indented or, pale wavy voided sable"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :line {:type :indented}
                   :humetty {:humetty? true}
                   :field {:type :heraldry.field.type/plain
                           :tincture :or}}
                  {:type :heraldry.ordinary.type/pale
                   :line {:type :wavy}
                   :voided {:voided? true}
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}]}

    "or a fess or, a pale sable, a chief vert"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :or}}
                  {:type :heraldry.ordinary.type/pale
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}
                  {:type :heraldry.ordinary.type/chief
                   :field {:type :heraldry.field.type/plain
                           :tincture :vert}}]}

    "or a fess or, a pale (sable, a chief vert)"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :or}}
                  {:type :heraldry.ordinary.type/pale
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable
                           :components [{:type :heraldry.ordinary.type/chief
                                         :field {:type :heraldry.field.type/plain
                                                 :tincture :vert}}]}}]}

    "or a fess or, a pale sable charged with a chief vert"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :or}}
                  {:type :heraldry.ordinary.type/pale
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable
                           :components [{:type :heraldry.ordinary.type/chief
                                         :field {:type :heraldry.field.type/plain
                                                 :tincture :vert}}]}}]}

    "per pale azure and argent, a fess sable"
    {:type :heraldry.field.type/per-pale
     :fields [{:type :heraldry.field.type/plain
               :tincture :azure}
              {:type :heraldry.field.type/plain
               :tincture :argent}]
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}]}

    "paly or and azure"
    {:type :heraldry.field.type/paly
     :fields [{:type :heraldry.field.type/plain
               :tincture :or}
              {:type :heraldry.field.type/plain
               :tincture :azure}
              {:type :heraldry.field.type/ref
               :index 0}
              {:type :heraldry.field.type/ref
               :index 1}
              {:type :heraldry.field.type/ref
               :index 0}
              {:type :heraldry.field.type/ref
               :index 1}]}

    "paly of three or and azure"
    {:type :heraldry.field.type/paly
     :layout {:num-fields-x 3}
     :fields [{:type :heraldry.field.type/plain
               :tincture :or}
              {:type :heraldry.field.type/plain
               :tincture :azure}
              {:type :heraldry.field.type/ref
               :index 0}]}

    "paly of 8 or and azure"
    {:type :heraldry.field.type/paly
     :layout {:num-fields-x 8}
     :fields [{:type :heraldry.field.type/plain
               :tincture :or}
              {:type :heraldry.field.type/plain
               :tincture :azure}
              {:type :heraldry.field.type/ref
               :index 0}
              {:type :heraldry.field.type/ref
               :index 1}
              {:type :heraldry.field.type/ref
               :index 0}
              {:type :heraldry.field.type/ref
               :index 1}
              {:type :heraldry.field.type/ref
               :index 0}
              {:type :heraldry.field.type/ref
               :index 1}]}

    "chequy of 2 or and azure"
    {:type :heraldry.field.type/chequy
     :layout {:num-fields-x 2}
     :fields [{:type :heraldry.field.type/plain
               :tincture :or}
              {:type :heraldry.field.type/plain
               :tincture :azure}]}

    "lozengy 2 tracts or and azure"
    {:type :heraldry.field.type/lozengy
     :layout {:num-fields-y 2}
     :fields [{:type :heraldry.field.type/plain
               :tincture :or}
              {:type :heraldry.field.type/plain
               :tincture :azure}]}

    "vairy 2 x4 or and azure"
    {:type :heraldry.field.type/vairy
     :layout {:num-fields-x 2
              :num-fields-y 4}
     :fields [{:type :heraldry.field.type/plain
               :tincture :or}
              {:type :heraldry.field.type/plain
               :tincture :azure}]}

    "papellony of three times twenty-eight tracts or and azure"
    {:type :heraldry.field.type/papellony
     :layout {:num-fields-x 3
              :num-fields-y 28}
     :fields [{:type :heraldry.field.type/plain
               :tincture :or}
              {:type :heraldry.field.type/plain
               :tincture :azure}]}
;;
    ))
