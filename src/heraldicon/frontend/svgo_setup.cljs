(ns heraldicon.frontend.svgo-setup
  (:require
   ["svgo-browser/lib/get-svgo-instance" :as getSvgoInstance]
   [cljs.core.async.interop :refer-macros [<p!]]
   [com.wsscode.async.async-cljs :refer [go]]
   [heraldicon.svg.core :as svg]))

;; There seems to be a bug in Inkscape, which strips ENTITY definitions from SVGs
;; saved with Adobe Illustrator, but still keeps those ENTITY references in the namespace
;; definitions, breaking SVGO when it tries to load such files after they were saved
;; in Inkscape.
;; I couldn't find a proper fix for that at the moment, but what does work is loading
;; such an original AI file, as it stores the references in some global repository inside
;; SVGO, which bleeds into subsequent parsing attempts. So loading a dummy AI file once
;; in the beginning will allow loading the "broken" Inkscape ones afterwards, hopefully
;; reducing the impact for users.
;; Finally, the xmlns aliases are important. And AI seems to use "x", "i", and "graph"
;; mainly. If others are observed in the wild, then they should be added.
(def ^:private minimal-adobe-illustrator-svg
  "<?xml version=\"1.0\" encoding=\"utf-8\"?>
<!-- Generator: Adobe Illustrator 24.0.1, SVG Export Plug-In . SVG Version: 6.00 Build 0)  -->
<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\" [
	<!ENTITY ns_extend \"http://ns.adobe.com/Extensibility/1.0/\">
	<!ENTITY ns_ai \"http://ns.adobe.com/AdobeIllustrator/10.0/\">
	<!ENTITY ns_graphs \"http://ns.adobe.com/Graphs/1.0/\">
	<!ENTITY ns_vars \"http://ns.adobe.com/Variables/1.0/\">
	<!ENTITY ns_imrep \"http://ns.adobe.com/ImageReplacement/1.0/\">
	<!ENTITY ns_sfw \"http://ns.adobe.com/SaveForWeb/1.0/\">
	<!ENTITY ns_custom \"http://ns.adobe.com/GenericCustomNamespace/1.0/\">
	<!ENTITY ns_adobe_xpath \"http://ns.adobe.com/XPath/1.0/\">
]>
<svg version=\"1.1\"
	 id=\"svg2\"
	 xmlns=\"http://www.w3.org/2000/svg\"
	 xmlns:x=\"&ns_extend;\"
	 xmlns:i=\"&ns_ai;\"
	 xmlns:graph=\"&ns_graphs;\">
	<g></g>
</svg>
")

(defn- setup-svg-loading []
  (svg/optimize minimal-adobe-illustrator-svg
                (fn [options data]
                  (go
                    (-> options
                        getSvgoInstance
                        (.optimize data)
                        <p!)))))

(defonce ^:export init
  (setup-svg-loading))
