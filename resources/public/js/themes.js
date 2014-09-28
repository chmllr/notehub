var $ = function(id){ return document.getElementById(id); }
var show = function(elem) { elem.style.display = "block" }

var themes = {
  "dark": {
    background: {
      normal: '#333',
      halftone: '#444'
    },
    foreground: {
      normal: '#ccc',
      halftone: '#bbb'
    },
    link: {
      fresh: '#6b8',
      visited: '#496',
      hover: '#7c9'
    }
  },
  "solarized-light": {
    background: {
      normal: '#fdf6e3',
      halftone: '#eee8d5'
    },
    foreground: {
      normal: '#657b83',
      halftone: '#839496'
    },
    link: {
      fresh: '#b58900',
      visited: '#cb4b16',
      hover: '#dc322f'
    }
  },
  "solarized-dark": {
    background: {
      normal: '#073642',
      halftone: '#002b36'
    },
    foreground: {
      normal: '#93a1a1',
      halftone: '#eee8d5'
    },
    link: {
      fresh: '#cb4b16',
      visited: '#b58900',
      hover: '#dc322f'
    }
  },
  "default": {
    background: {
      normal: '#fff',
      halftone: '#efefef'
    },
    foreground: {
      normal: '#333',
      halftone: '#888'
    },
    link: {
      fresh: '#097',
      visited: '#054',
      hover: '#0a8'
    }
  }
};


var ui = { theme: "default" };
if (location.search.length > 0) {
  location.search.slice(1).split("&").reduce(function(acc, e){
    var p = e.split("=");
    acc[p[0]] = p[1];
    return acc
  }, ui);
}

var vars = {
  '@background': themes[ui.theme].background.normal,
  '@background_halftone': themes[ui.theme].background.halftone,
  '@foreground': themes[ui.theme].foreground.normal,
  '@foreground_halftone': themes[ui.theme].foreground.halftone,
  '@link_fresh': themes[ui.theme].link.fresh,
  '@link_visited': themes[ui.theme].link.visited,
  '@link_hover': themes[ui.theme].link.hover
};

var fontURL = "http://fonts.googleapis.com/" +
    "css?family=PT+Serif:700|Noticia+Text:700%s" +
    "&subset=latin,cyrillic",
    injection = "";

if(ui["header-font"] || ui["text-font"]) {
  injection = ["header-font", "text-font"].reduce(function(acc, font){
    if(ui[font]) {
      vars['@' + font.replace(/-/, "_")] = ui[font].replace(/\+/g," ");
      return acc + "|" + ui[font];
    } else return acc;
  }, "");
}

if(ui["text-size"]) vars["@font_size"] = ui["text-size"] + "em";

fontURL = fontURL.replace(/%s/, injection);
var fileref = document.createElement("link")
fileref.setAttribute("rel", "stylesheet")
fileref.setAttribute("type", "text/css")
fileref.setAttribute("href", fontURL)
document.getElementsByTagName("head")[0].appendChild(fileref)

less.modifyVars(vars);

function showLinks(){
  var links = $("links");
  if(links){
    if(window.innerHeight * 0.85 >= document.body.clientHeight) {
      links.style.position = "fixed";
      links.style.bottom = 0;
    }
    show(links);
  }
}

// for the case if main.js is not loaded
var onLoad = showLinks;
